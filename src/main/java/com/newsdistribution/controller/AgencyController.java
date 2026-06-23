package com.newsdistribution.controller;

import com.newsdistribution.entity.WebOrder;
import com.newsdistribution.entity.WebUser;
import com.newsdistribution.repository.WebUserRepository;
import com.newsdistribution.service.AgencyService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/agency")
public class AgencyController {

    private final AgencyService agencyService;
    private final WebUserRepository userRepo;
    private final JdbcTemplate jdbcB;

    public AgencyController(AgencyService agencyService, 
                            WebUserRepository userRepo, 
                            @Qualifier("jdbcB") JdbcTemplate jdbcB) {
        this.agencyService = agencyService;
        this.userRepo = userRepo;
        this.jdbcB = jdbcB;
    }

    private String getMakh(Authentication auth) {
        return (String) auth.getDetails();
    }

    private Integer getUserId(Authentication auth) {
        if (auth == null) return null;
        String username = auth.getName();
        return userRepo.findByUsername(username)
                .map(WebUser::getId)
                .orElse(null);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        return ResponseEntity.ok(agencyService.getDashboard(getMakh(auth)));
    }

    @GetMapping("/publications")
    public ResponseEntity<?> publications() {
        return ResponseEntity.ok(agencyService.getPublications());
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(Authentication auth) {
        return ResponseEntity.ok(Map.of("orders", agencyService.getOrdersByMakh(getMakh(auth))));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            var order = agencyService.createOrder(getMakh(auth), getUserId(auth), body);
            return ResponseEntity.ok(Map.of("orderCode", order.getOrderCode(), "id", order.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/confirm-received")
    public ResponseEntity<?> confirmReceived(@PathVariable Integer id, Authentication auth) {
        String makh = getMakh(auth);
        int updated = jdbcB.update("UPDATE web_orders SET delivery_status = 'RECEIVED' WHERE id = ? AND makh = ?", id, makh);
        if (updated > 0) {
            String orderCode = jdbcB.queryForObject("SELECT order_code FROM web_orders WHERE id = ?", String.class, id);
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            jdbcB.update("INSERT INTO web_order_history (order_code, nguoi_thuc_hien, loai_hanh_dong, ghi_chu_chi_tiet) VALUES (?, ?, ?, ?)",
                orderCode, username + " (Đại lý)", "XÁC NHẬN NHẬN HÀNG", "Đại lý xác nhận đã nhận đủ hàng.");
        }
        return ResponseEntity.ok(Map.of("success", updated > 0));
    }

    @PostMapping("/orders/{id}/upload-receipt")
    public ResponseEntity<?> uploadReceipt(@PathVariable Integer id, @RequestParam("file") org.springframework.web.multipart.MultipartFile file, Authentication auth) {
        try {
            String makh = getMakh(auth);
            Integer count = jdbcB.queryForObject("SELECT COUNT(*) FROM web_orders WHERE id = ? AND makh = ?", Integer.class, id, makh);
            if (count == null || count == 0) return ResponseEntity.badRequest().body(Map.of("error", "Order not found"));
            
            String originalName = file.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
            String newFileName = "receipt_" + id + "_" + System.currentTimeMillis() + extension;
            java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }
            java.nio.file.Path filePath = uploadPath.resolve(newFileName);
            file.transferTo(filePath.toFile());
            
            String receiptUrl = "/uploads/" + newFileName;
            jdbcB.update("UPDATE web_orders SET payment_status = N'CHỜ XÁC NHẬN', receipt_url = ? WHERE id = ?", receiptUrl, id);
            
            String orderCode = jdbcB.queryForObject("SELECT order_code FROM web_orders WHERE id = ?", String.class, id);
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            jdbcB.update("INSERT INTO web_order_history (order_code, nguoi_thuc_hien, loai_hanh_dong, ghi_chu_chi_tiet) VALUES (?, ?, ?, ?)",
                orderCode, username + " (Đại lý)", "NỘP CHỨNG TỪ", "Đã nộp chứng từ thanh toán: " + receiptUrl);
                
            return ResponseEntity.ok(Map.of("success", true, "receiptUrl", receiptUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/pay-online")
    public ResponseEntity<?> payOnline(@PathVariable Integer id, Authentication auth) {
        try {
            String makh = getMakh(auth);
            Integer count = jdbcB.queryForObject("SELECT COUNT(*) FROM web_orders WHERE id = ? AND makh = ?", Integer.class, id, makh);
            if (count == null || count == 0) return ResponseEntity.badRequest().body(Map.of("error", "Order not found"));
            
            jdbcB.update("UPDATE web_orders SET payment_status = 'PAID' WHERE id = ?", id);
            
            String orderCode = jdbcB.queryForObject("SELECT order_code FROM web_orders WHERE id = ?", String.class, id);
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            jdbcB.update("INSERT INTO web_order_history (order_code, nguoi_thuc_hien, loai_hanh_dong, ghi_chu_chi_tiet) VALUES (?, ?, ?, ?)",
                orderCode, username + " (Khách hàng)", "THANH TOÁN ONLINE", "Đã thanh toán trực tuyến thành công");
                
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debt")
    public ResponseEntity<?> getDebt(Authentication auth) {
        try {
            String makh = getMakh(auth);
            Double hanMucNo = null;
            try {
                // Use BigDecimal for han_muc_no to prevent cast issues
                java.math.BigDecimal val = jdbcB.queryForObject("SELECT han_muc_no FROM web_users WHERE makh = ?", java.math.BigDecimal.class, makh);
                if (val != null) hanMucNo = val.doubleValue();
            } catch (Exception e) {}
            if (hanMucNo == null) hanMucNo = 0.0;
            
            java.util.List<Map<String, Object>> unpaidOrders = jdbcB.queryForList(
                "SELECT o.id, o.order_code, o.created_at, DATEDIFF(day, o.created_at, GETDATE()) as days_unpaid, " +
                "ISNULL(SUM(i.thanh_tien), 0) as amount " +
                "FROM web_orders o " +
                "LEFT JOIN web_order_items i ON o.id = i.order_id " +
                "WHERE o.makh = ? AND (o.payment_status IS NULL OR o.payment_status = 'UNPAID') " +
                "GROUP BY o.id, o.order_code, o.created_at", makh);
                
            double totalDebt = unpaidOrders.stream()
                .mapToDouble(m -> {
                    Object amt = m.get("amount");
                    if (amt == null) return 0.0;
                    if (amt instanceof Number) return ((Number)amt).doubleValue();
                    return Double.parseDouble(amt.toString());
                })
                .sum();
                
            double remaining = hanMucNo - totalDebt;
            
            return ResponseEntity.ok(Map.of(
                "tongNo", totalDebt,
                "hanMuc", hanMucNo,
                "conLai", remaining,
                "unpaidOrders", unpaidOrders
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests(Authentication auth) {
        return ResponseEntity.ok(jdbcB.queryForList(
            "SELECT id, order_code, loai_yeu_cau, noi_dung, trang_thai, admin_reply, created_at " +
            "FROM web_requests WHERE makh = ? ORDER BY created_at DESC", getMakh(auth)
        ));
    }

    @GetMapping("/analytics/trend")
    public ResponseEntity<?> getTrend(Authentication auth) {
        java.util.List<Map<String, Object>> data = jdbcB.queryForList(
            "SELECT FORMAT(o.created_at, 'MM/yyyy') as month, " +
            "SUM(i.thanh_tien) as total " +
            "FROM web_orders o " +
            "JOIN web_order_items i ON o.id = i.order_id " +
            "WHERE o.makh = ? AND o.created_at >= DATEADD(month, -6, GETDATE()) " +
            "GROUP BY FORMAT(o.created_at, 'MM/yyyy'), YEAR(o.created_at), MONTH(o.created_at) " +
            "ORDER BY YEAR(o.created_at), MONTH(o.created_at)", getMakh(auth)
        );
        return ResponseEntity.ok(data);
    }

    @PostMapping("/requests")
    public ResponseEntity<?> createRequest(@RequestBody Map<String, String> payload, Authentication auth) {
        try {
            String makh = getMakh(auth);
            String orderCode = payload.get("order_code");
            String loaiYeuCau = payload.get("loai_yeu_cau");
            String noiDung = payload.get("noi_dung");
            
            jdbcB.update("INSERT INTO web_requests (makh, order_code, loai_yeu_cau, noi_dung, trang_thai) VALUES (?, ?, ?, ?, 'CHỜ DUYỆT')",
                makh, orderCode, loaiYeuCau, noiDung);
                
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            jdbcB.update("INSERT INTO web_order_history (order_code, nguoi_thuc_hien, loai_hanh_dong, ghi_chu_chi_tiet) VALUES (?, ?, ?, ?)",
                orderCode, username + " (Đại lý)", "TẠO YÊU CẦU", "Loại: " + loaiYeuCau + " - " + noiDung);
                
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<?> getTemplates(Authentication auth) {
        String makh = getMakh(auth);
        java.util.List<Map<String, Object>> templates = jdbcB.queryForList(
            "SELECT id, template_name, created_at FROM web_order_templates WHERE makh = ?", makh);
            
        for (Map<String, Object> t : templates) {
            Integer tId = (Integer) t.get("id");
            java.util.List<Map<String, Object>> items = jdbcB.queryForList(
                "SELECT i.ma_bao, i.so_luong, " +
                "(SELECT ten FROM m_bao b WHERE b.maBao = i.ma_bao COLLATE DATABASE_DEFAULT) as ten_bao " +
                "FROM web_order_template_items i WHERE i.template_id = ?", tId);
            t.put("items", items);
        }
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/templates")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> payload, Authentication auth) {
        try {
            String makh = getMakh(auth);
            String templateName = (String) payload.get("templateName");
            java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) payload.get("items");
            
            jdbcB.update("INSERT INTO web_order_templates (makh, template_name) VALUES (?, ?)", makh, templateName);
            Integer templateId = jdbcB.queryForObject("SELECT IDENT_CURRENT('web_order_templates')", Integer.class);
            
            for (Map<String, Object> item : items) {
                jdbcB.update("INSERT INTO web_order_template_items (template_id, ma_bao, so_luong) VALUES (?, ?, ?)", 
                    templateId, item.get("maBao"), item.get("soLuong"));
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Integer id, Authentication auth) {
        String makh = getMakh(auth);
        int deleted = jdbcB.update("DELETE FROM web_order_templates WHERE id = ? AND makh = ?", id, makh);
        return ResponseEntity.ok(Map.of("success", deleted > 0));
    }

    @GetMapping("/forecast/{maBao}")
    public ResponseEntity<?> getForecast(@PathVariable String maBao, Authentication auth) {
        String makh = getMakh(auth);
        try {
            Double avgQty = jdbcB.queryForObject(
                "SELECT AVG(CAST(i.so_luong AS FLOAT)) FROM web_order_items i " +
                "JOIN web_orders o ON i.order_id = o.id " +
                "WHERE o.makh = ? AND i.ma_bao = ? " +
                "AND o.created_at >= DATEADD(month, -3, GETDATE())", Double.class, makh, maBao);
                
            if (avgQty == null) avgQty = 0.0;
            int suggested = (int) Math.ceil(avgQty * 1.05);
            return ResponseEntity.ok(Map.of("suggested", suggested));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("suggested", 0));
        }
    }
}
