package com.newsdistribution.service;

import com.newsdistribution.entity.*;
import com.newsdistribution.repository.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AgencyService {

    private final JdbcTemplate jdbcC;
    private final JdbcTemplate jdbcB;
    private final WebOrderRepository orderRepo;
    private final WebOrderItemRepository itemRepo;
    private final SimpMessagingTemplate ws;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${admin.notification.email:}")
    private String adminEmail;

    public AgencyService(@Qualifier("jdbcC") JdbcTemplate jdbcC,
                         @Qualifier("jdbcB") JdbcTemplate jdbcB,
                         WebOrderRepository orderRepo,
                         WebOrderItemRepository itemRepo,
                         SimpMessagingTemplate ws,
                         EmailService emailService) {
        this.jdbcC = jdbcC;
        this.jdbcB = jdbcB;
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.ws = ws;
        this.emailService = emailService;
    }

    public Map<String, Object> getDashboard(String makh) {
        // Tổng tiền
        Double tongTien = jdbcC.queryForObject(
            "SELECT ISNULL(SUM(tong_tien),0) FROM m_hoa_don WHERE makh=? AND source='WINFORM'",
            Double.class, makh);
        // Tổng số báo
        Integer tongBao = jdbcC.queryForObject(
            "SELECT ISNULL(SUM(tong_so_bao),0) FROM m_hoa_don WHERE makh=? AND source='WINFORM'",
            Integer.class, makh);
        // Số hóa đơn
        Integer soHD = jdbcB.queryForObject(
            "SELECT COUNT(*) FROM web_orders WHERE makh=?",
            Integer.class, makh);
        // Top 5 đơn gần nhất (Web)
        var donGanNhat = jdbcB.queryForList(
            "SELECT TOP 5 o.order_code as sohd, o.created_at as ngayLapPhieu, " +
            "(SELECT ISNULL(SUM(thanh_tien),0) FROM web_order_items i WHERE i.order_id = o.id) as tong_tien, " +
            "o.sync_status as thanhToan " +
            "FROM web_orders o WHERE o.makh=? ORDER BY o.created_at DESC",
            makh);
        return Map.of(
            "tongTien", tongTien != null ? tongTien : 0.0,
            "tongBao", tongBao != null ? tongBao : 0,
            "soHoaDon", soHD != null ? soHD : 0,
            "donGanNhat", donGanNhat
        );
    }

    public List<Map<String, Object>> getPublications() {
        return jdbcC.queryForList(
            "SELECT maBao,ten,DVT,donGia,ngayBatDau,thu1,thu2,thu3,thu4,thu5,thu6,thu7 FROM m_bao WHERE donGia IS NOT NULL ORDER BY ten"
        );
    }

    public List<WebOrder> getOrdersByMakh(String makh) {
        return orderRepo.findByMakhOrderByCreatedAtDesc(makh);
    }

    @Transactional
    public WebOrder createOrder(String makh, Integer userId, Map<String, Object> body) {
        String orderCode = "WEB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        
        String orderType = "THUONG";
        if (Boolean.TRUE.equals(body.get("isBoSung"))) {
            orderCode += "-BS";
            orderType = "BO_SUNG";
        }

        // Pessimistic Lock on web_users to prevent race condition when checking credit limit
        Double hanMucNo = jdbcB.queryForObject(
            "SELECT han_muc_no FROM web_users WITH (UPDLOCK, ROWLOCK) WHERE makh = ?", 
            Double.class, makh);
        if (hanMucNo == null) hanMucNo = 0.0;
        
        Double tongNoHienTai = jdbcB.queryForObject(
            "SELECT ISNULL(SUM(thanh_tien),0) FROM web_order_items i " +
            "JOIN web_orders o ON i.order_id = o.id " +
            "WHERE o.makh = ? AND (o.payment_status IS NULL OR o.payment_status = 'UNPAID')", 
            Double.class, makh);
        if (tongNoHienTai == null) tongNoHienTai = 0.0;

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        BigDecimal newOrderTotal = BigDecimal.ZERO;
        List<Map<String, Object>> validatedItems = new ArrayList<>();
        
        for (var item : items) {
            var maBao = (String) item.get("maBao");
            List<Map<String, Object>> baoInfos = jdbcC.queryForList("SELECT ten, donGia FROM m_bao WHERE maBao=? AND donGia IS NOT NULL", maBao);
            if (baoInfos.isEmpty()) {
                throw new RuntimeException("Đầu báo " + maBao + " không tồn tại hoặc đã ngừng phát hành.");
            }
            var baoInfo = baoInfos.get(0);
            
            int soLuong = Integer.parseInt(item.get("soLuong").toString());
            if (soLuong <= 0) {
                throw new RuntimeException("Số lượng đặt phải lớn hơn 0.");
            }
            BigDecimal donGia = new BigDecimal(baoInfo.get("donGia").toString());
            newOrderTotal = newOrderTotal.add(donGia.multiply(BigDecimal.valueOf(soLuong)));
            
            Map<String, Object> validItem = new HashMap<>(item);
            validItem.put("_baoInfo", baoInfo);
            validItem.put("_donGia", donGia);
            validatedItems.add(validItem);
        }

        // The user requested to remove the credit limit check so agencies can order freely
        // if (tongNoHienTai + newOrderTotal.doubleValue() > hanMucNo) {
        //     throw new RuntimeException("Vượt quá hạn mức tín dụng cho phép! Hạn mức còn lại: " + (hanMucNo - tongNoHienTai) + "đ.");
        // }

        WebOrder order = WebOrder.builder()
            .orderCode(orderCode)
            .userId(userId != null ? userId : 1) // default fallback userId
            .makh(makh)
            .tuNgay(java.time.LocalDate.parse((String) body.get("tuNgay")))
            .denNgay(java.time.LocalDate.parse((String) body.get("denNgay")))
            .ghiChu((String) body.get("ghiChu"))
            .syncStatus("PENDING")
            .orderType(orderType)
            .createdAt(LocalDateTime.now())
            .build();

        order = orderRepo.save(order);
        
        // Cập nhật default status
        jdbcB.update("UPDATE web_orders SET payment_status = 'UNPAID', delivery_status = 'PENDING' WHERE id = ?", order.getId());

        List<WebOrderItem> savedItems = new ArrayList<>();
        
        for (var item : validatedItems) {
            var maBao = (String) item.get("maBao");
            var baoInfo = (Map<String, Object>) item.get("_baoInfo");
            int soLuong = Integer.parseInt(item.get("soLuong").toString());
            BigDecimal donGia = (BigDecimal) item.get("_donGia");
            BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(soLuong));

            WebOrderItem orderItem = WebOrderItem.builder()
                .orderId(order.getId())
                .maBao(maBao)
                .tenBao(baoInfo.get("ten").toString())
                .donGia(donGia)
                .soLuong(soLuong)
                .thanhTien(thanhTien)
                .build();
                
            savedItems.add(itemRepo.save(orderItem));
        }

        order.setItems(savedItems);

        // WebSocket: thông báo admin có đơn mới
        ws.convertAndSend("/topic/orders", Map.of(
            "type", "NEW_ORDER",
            "orderCode", orderCode,
            "makh", makh,
            "orderType", orderType
        ));

        // Email Notification
        if (adminEmail != null && !adminEmail.isBlank()) {
            emailService.sendOrderNotification(
                adminEmail, 
                makh, 
                orderCode, 
                orderType, 
                String.format("%,.0f", newOrderTotal.doubleValue())
            );
        }

        return order;
    }
}
