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
    private final WebOrderRepository orderRepo;
    private final WebOrderItemRepository itemRepo;
    private final SimpMessagingTemplate ws;

    public AgencyService(@Qualifier("jdbcC") JdbcTemplate jdbcC,
                         WebOrderRepository orderRepo,
                         WebOrderItemRepository itemRepo,
                         SimpMessagingTemplate ws) {
        this.jdbcC = jdbcC;
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.ws = ws;
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
        Integer soHD = jdbcC.queryForObject(
            "SELECT COUNT(*) FROM m_hoa_don WHERE makh=? AND source='WINFORM'",
            Integer.class, makh);
        // Top 5 đơn gần nhất
        var donGanNhat = jdbcC.queryForList(
            "SELECT TOP 5 sohd, ngayLapPhieu, tong_tien, thanhToan FROM m_hoa_don WHERE makh=? ORDER BY ngayLapPhieu DESC",
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

        WebOrder order = WebOrder.builder()
            .orderCode(orderCode)
            .userId(userId != null ? userId : 1) // default fallback userId
            .makh(makh)
            .tuNgay(java.time.LocalDate.parse((String) body.get("tuNgay")))
            .denNgay(java.time.LocalDate.parse((String) body.get("denNgay")))
            .ghiChu((String) body.get("ghiChu"))
            .syncStatus("PENDING")
            .createdAt(LocalDateTime.now())
            .build();

        order = orderRepo.save(order);

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        List<WebOrderItem> savedItems = new ArrayList<>();
        
        for (var item : items) {
            var maBao = (String) item.get("maBao");
            var baoInfo = jdbcC.queryForMap("SELECT ten, donGia FROM m_bao WHERE maBao=?", maBao);
            int soLuong = Integer.parseInt(item.get("soLuong").toString());
            BigDecimal donGia = new BigDecimal(baoInfo.get("donGia").toString());
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
            "makh", makh
        ));

        return order;
    }
}
