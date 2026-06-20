package com.newsdistribution.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private final JdbcTemplate jdbcB;

    public AdminService(@Qualifier("jdbcB") JdbcTemplate jdbcB) {
        this.jdbcB = jdbcB;
    }

    @Transactional(rollbackFor = Exception.class)
    public String approveRequest(Integer requestId, Map<String, Object> payload, String username) {
        // Lấy thông tin request bằng UPDLOCK để tránh admin ấn 2 lần liên tiếp gây race condition
        Map<String, Object> request = jdbcB.queryForMap("SELECT * FROM web_requests WITH (UPDLOCK) WHERE id = ? AND trang_thai = N'CHỜ DUYỆT'", requestId);
        if (request == null || request.isEmpty()) {
            throw new RuntimeException("Yêu cầu không tồn tại hoặc đã được xử lý!");
        }

        String orderCode = (String) request.get("order_code");
        String makh = (String) request.get("makh");
        
        // Lấy Original Order info
        Map<String, Object> originalOrder = jdbcB.queryForMap("SELECT * FROM web_orders WHERE order_code = ?", orderCode);
        Integer originalUserId = (Integer) originalOrder.get("user_id");
        
        // Approval params
        String adminReply = (String) payload.get("admin_reply");
        String maBao = (String) payload.get("ma_bao");
        String tenBao = (String) payload.get("ten_bao");
        Integer soLuong = payload.get("so_luong") != null ? Integer.parseInt(payload.get("so_luong").toString()) : 0;
        
        // Cập nhật trạng thái Request
        jdbcB.update("UPDATE web_requests SET trang_thai = N'ĐÃ DUYỆT', admin_reply = ? WHERE id = ?", adminReply, requestId);
        
        String loaiYeuCau = (String) request.get("loai_yeu_cau");
        Object refundObj = payload.get("refund_amount");
        if (refundObj != null && ("Trả báo".equals(loaiYeuCau) || "Báo lỗi/hỏng".equals(loaiYeuCau))) {
            double refundAmt = Double.parseDouble(refundObj.toString());
            if (refundAmt > 0) {
                jdbcB.update("UPDATE web_users SET han_muc_no = han_muc_no + ? WHERE makh = ?", refundAmt, makh);
            }
        }
        
        // Create Supplementary Order if applicable (e.g., đặt bổ sung)
        String bsOrderCode = null;
        if (maBao != null && soLuong > 0) {
            bsOrderCode = orderCode + "-BS" + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
            jdbcB.update("INSERT INTO web_orders (order_code, user_id, makh, tu_ngay, den_ngay, ghi_chu, sync_status, created_at, payment_status, delivery_status) VALUES (?, ?, ?, GETDATE(), GETDATE(), ?, 'SYNCED', GETDATE(), 'UNPAID', 'PENDING')",
                bsOrderCode, originalUserId, makh, "Đơn xử lý theo yêu cầu #" + requestId);
            
            Integer newOrderId = jdbcB.queryForObject("SELECT id FROM web_orders WHERE order_code = ?", Integer.class, bsOrderCode);
            
            // Insert Item (0 dong nếu bù hàng)
            jdbcB.update("INSERT INTO web_order_items (order_id, ma_bao, ten_bao, don_gia, so_luong, thanh_tien) VALUES (?, ?, ?, 0, ?, 0)",
                newOrderId, maBao, tenBao, soLuong);
        }
            
        // Log History
        jdbcB.update("INSERT INTO web_order_history (order_code, nguoi_thuc_hien, loai_hanh_dong, ghi_chu_chi_tiet) VALUES (?, ?, ?, ?)",
            orderCode, username, "DUYỆT YÊU CẦU", "Đã xử lý yêu cầu. Phản hồi: " + adminReply);
            
        return bsOrderCode;
    }
}
