package com.newsdistribution.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JdbcTemplate jdbcB;
    private final com.newsdistribution.service.AdminService adminService;

    public AdminController(@Qualifier("jdbcB") JdbcTemplate jdbcB, com.newsdistribution.service.AdminService adminService) {
        this.jdbcB = jdbcB;
        this.adminService = adminService;
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<?> getTableData(@PathVariable String tableName) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid table name"));
        }

        try {
            String sql;
            if ("web_users".equalsIgnoreCase(tableName)) {
                sql = "SELECT id, username, role, makh, ten_hien_thi, email, is_active, ten_doanh_nghiep, ma_so_thue, dia_chi, so_dien_thoai, created_at, updated_at FROM web_users WHERE role = 'AGENCY' ORDER BY id DESC";
            } else {
                sql = "SELECT TOP 100 * FROM " + tableName + " ORDER BY id DESC";
            }
            List<Map<String, Object>> data = jdbcB.queryForList(sql);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy dữ liệu bảng: " + e.getMessage()));
        }
    }

    @GetMapping("/revenue/daily")
    public ResponseEntity<?> getDailyRevenue() {
        try {
            String sql = """
                WITH DateCTE AS (
                    SELECT CAST(DATEADD(day, -29, GETDATE()) AS DATE) AS d
                    UNION ALL
                    SELECT DATEADD(day, 1, d)
                    FROM DateCTE
                    WHERE d < CAST(GETDATE() AS DATE)
                )
                SELECT d.d as label, ISNULL(SUM(i.thanh_tien), 0) as value
                FROM DateCTE d
                LEFT JOIN web_orders o ON CAST(o.created_at AS DATE) = d.d
                LEFT JOIN web_order_items i ON o.id = i.order_id
                GROUP BY d.d
                ORDER BY d.d ASC
                OPTION (MAXRECURSION 365)
                """;
            return ResponseEntity.ok(jdbcB.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analytics/top-products")
    public ResponseEntity<?> getTopProducts() {
        try {
            String sql = "SELECT TOP 5 ten_bao as label, SUM(so_luong) as value " +
                         "FROM web_order_items " +
                         "GROUP BY ten_bao " +
                         "ORDER BY value DESC";
            return ResponseEntity.ok(jdbcB.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/update-status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        try {
            String deliveryStatus = payload.get("delivery_status");
            String paymentStatus = payload.get("payment_status");
            
            String updateSql = "UPDATE web_orders SET ";
            boolean hasUpdate = false;
            if (deliveryStatus != null) {
                updateSql += "delivery_status = '" + deliveryStatus + "'";
                hasUpdate = true;
            }
            if (paymentStatus != null) {
                if (hasUpdate) updateSql += ", ";
                updateSql += "payment_status = '" + paymentStatus + "'";
                hasUpdate = true;
            }
            updateSql += " WHERE id = ?";
            
            if (hasUpdate) {
                jdbcB.update(updateSql, id);
                String orderCode = jdbcB.queryForObject("SELECT order_code FROM web_orders WHERE id = ?", String.class, id);
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                jdbcB.update("INSERT INTO web_order_history (order_code, nguoi_thuc_hien, loai_hanh_dong, ghi_chu_chi_tiet) VALUES (?, ?, ?, ?)",
                    orderCode, username + " (Admin)", "CẬP NHẬT TRẠNG THÁI", "Delivery: " + deliveryStatus + ", Payment: " + paymentStatus);
            }
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/analytics/top-agencies")
    public ResponseEntity<?> getTopAgencies() {
        try {
            String sql = "SELECT TOP 5 COALESCE(u.ten_doanh_nghiep, u.ten_hien_thi) as label, SUM(i.thanh_tien) as value " +
                         "FROM web_orders o " +
                         "JOIN web_order_items i ON o.id = i.order_id " +
                         "JOIN web_users u ON o.makh = u.makh " +
                         "GROUP BY COALESCE(u.ten_doanh_nghiep, u.ten_hien_thi) " +
                         "ORDER BY value DESC";
            return ResponseEntity.ok(jdbcB.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analytics/compare")
    public ResponseEntity<?> getComparison() {
        try {
            String sqlToday = "SELECT COUNT(DISTINCT o.id) as orders, ISNULL(SUM(i.thanh_tien), 0) as revenue " +
                              "FROM web_orders o LEFT JOIN web_order_items i ON o.id = i.order_id " +
                              "WHERE CONVERT(date, o.created_at) = CONVERT(date, GETDATE())";
                              
            String sqlYesterday = "SELECT COUNT(DISTINCT o.id) as orders, ISNULL(SUM(i.thanh_tien), 0) as revenue " +
                                  "FROM web_orders o LEFT JOIN web_order_items i ON o.id = i.order_id " +
                                  "WHERE CONVERT(date, o.created_at) = CONVERT(date, DATEADD(day, -1, GETDATE()))";
            
            Map<String, Object> today = jdbcB.queryForMap(sqlToday);
            Map<String, Object> yesterday = jdbcB.queryForMap(sqlYesterday);
            
            return ResponseEntity.ok(Map.of("today", today, "yesterday", yesterday));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/orders/{orderId}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable Integer orderId) {
        try {
            String sql = "SELECT * FROM web_order_items WHERE order_id = ?";
            return ResponseEntity.ok(jdbcB.queryForList(sql, orderId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests() {
        try {
            return ResponseEntity.ok(jdbcB.queryForList("SELECT * FROM web_requests ORDER BY id DESC"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/requests")
    public ResponseEntity<?> createRequest(@RequestBody Map<String, String> payload) {
        try {
            String makh = payload.get("makh");
            String orderCode = payload.get("order_code");
            String loaiYeuCau = payload.get("loai_yeu_cau");
            String noiDung = payload.get("noi_dung");
            
            jdbcB.update("INSERT INTO web_requests (makh, order_code, loai_yeu_cau, noi_dung, trang_thai) VALUES (?, ?, ?, ?, 'CHỜ DUYỆT')",
                makh, orderCode, loaiYeuCau, noiDung);
                
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            jdbcB.update("INSERT INTO web_order_history (order_code, nguoi_thuc_hien, loai_hanh_dong, ghi_chu_chi_tiet) VALUES (?, ?, ?, ?)",
                orderCode, username, "TẠO YÊU CẦU", "Loại: " + loaiYeuCau + " - " + noiDung);
                
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            String newOrderCode = adminService.approveRequest(id, payload, username);
            return ResponseEntity.ok(Map.of("success", true, "new_order_code", newOrderCode != null ? newOrderCode : ""));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        try {
            return ResponseEntity.ok(jdbcB.queryForList("SELECT * FROM web_order_history ORDER BY id DESC"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
