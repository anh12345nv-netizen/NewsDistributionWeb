package com.newsdistribution.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MockDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataSeeder.class);
    private final JdbcTemplate jdbcB;
    private final JdbcTemplate jdbcC;

    public MockDataSeeder(@Qualifier("jdbcB") JdbcTemplate jdbcB, @Qualifier("jdbcC") JdbcTemplate jdbcC) {
        this.jdbcB = jdbcB;
        this.jdbcC = jdbcC;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Bắt đầu tạo dữ liệu giả lập cho 50 đại lý...");
        Random random = new Random();

        // Xóa dữ liệu cũ đã sinh trước đó
        try {
            jdbcB.update("DELETE FROM web_order_items");
            jdbcB.update("DELETE FROM web_orders");
            jdbcC.update("DELETE FROM m_hoa_don WHERE makh LIKE 'DL%'");
        } catch (Exception e) {
            log.warn("Lỗi khi xóa dữ liệu cũ: {}", e.getMessage());
        }

        // Sửa lỗi font chữ cho web_users
        try {
            jdbcB.update("ALTER TABLE web_users ALTER COLUMN ten_hien_thi NVARCHAR(255)");
            jdbcB.update("ALTER TABLE web_users ALTER COLUMN ten_doanh_nghiep NVARCHAR(255)");
            jdbcB.update("ALTER TABLE web_users ALTER COLUMN dia_chi NVARCHAR(MAX)");
            
            // Bổ sung han_muc_no
            try {
                jdbcB.update("ALTER TABLE web_users ADD han_muc_no DECIMAL(18,2) DEFAULT 50000000.00");
            } catch (Exception e) {}
            
        } catch (Exception e) {
            log.warn("Bảng web_users đã có các cột này, bỏ qua alter.");
        }

        // Tạo bảng web_requests và web_order_history
        try {
            jdbcB.update("IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='web_requests' and xtype='U') " +
                "CREATE TABLE web_requests (" +
                "id INT IDENTITY(1,1) PRIMARY KEY, " +
                "makh NVARCHAR(50), " +
                "order_code NVARCHAR(100), " +
                "loai_yeu_cau NVARCHAR(50), " +
                "noi_dung NVARCHAR(MAX), " +
                "trang_thai NVARCHAR(50) DEFAULT 'CHỜ DUYỆT', " +
                "admin_reply NVARCHAR(MAX), " +
                "created_at DATETIME DEFAULT GETDATE())");
                
            // Bổ sung các cột trạng thái cho web_orders
            try {
                jdbcB.update("ALTER TABLE web_orders ADD payment_status NVARCHAR(50) DEFAULT 'UNPAID'");
                jdbcB.update("ALTER TABLE web_orders ADD delivery_status NVARCHAR(50) DEFAULT 'PENDING'");
            } catch (Exception e) {}
                
            jdbcB.update("IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='web_order_history' and xtype='U') " +
                "CREATE TABLE web_order_history (" +
                "id INT IDENTITY(1,1) PRIMARY KEY, " +
                "order_code NVARCHAR(100), " +
                "nguoi_thuc_hien NVARCHAR(100), " +
                "loai_hanh_dong NVARCHAR(100), " +
                "ghi_chu_chi_tiet NVARCHAR(MAX), " +
                "created_at DATETIME DEFAULT GETDATE())");
        } catch (Exception e) {
            log.error("Lỗi khi tạo bảng mới: {}", e.getMessage());
        }

        for (int i = 1; i <= 50; i++) {
            String username = "daily" + i;
            String tenHienThi = "Đại lý " + i;
            String tenDoanhNghiep = "Công ty TNHH Đại lý " + i;
            String diaChi = "Số " + i + ", Đường Lê Lợi, Phường Bến Nghé, Quận 1, TP.HCM";
            jdbcB.update("UPDATE web_users SET ten_hien_thi = ?, ten_doanh_nghiep = ?, dia_chi = ?, han_muc_no = 50000000.00 WHERE username = ?", 
                tenHienThi, tenDoanhNghiep, diaChi, username);
        }

        // Lấy danh sách báo thực tế từ Master DB
        var listBao = jdbcC.queryForList("SELECT maBao, ten, donGia FROM m_bao WHERE donGia IS NOT NULL");
        if (listBao.isEmpty()) {
            log.warn("Không tìm thấy báo nào trong CSDL, bỏ qua tạo đơn hàng");
            return;
        }

        // 1. Cập nhật makh cho web_users (từ daily1 đến daily50)
        for (int i = 1; i <= 50; i++) {
            String username = "daily" + i;
            String makh = String.format("DL%03d", i);
            jdbcB.update("UPDATE web_users SET makh = ? WHERE username = ?", makh, username);

            // Sinh dữ liệu hóa đơn (m_hoa_don) trên DB C để hiện thị Dashboard
            int soHoaDon = 5 + random.nextInt(10); // 5 - 14 hóa đơn mỗi đại lý
            for (int j = 0; j < soHoaDon; j++) {
                String sohd = "HD" + makh + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                LocalDate ngayLap = LocalDate.now().minusDays(random.nextInt(60));
                int tongSoBao = 100 + random.nextInt(500);
                double tongTien = tongSoBao * 5000.0; // Tạm tính trung bình 5000đ/báo cho dashboard
                double thanhToan = tongTien; // Đã thanh toán hết
                
                try {
                    jdbcC.update("INSERT INTO m_hoa_don (sohd, ngayLapPhieu, tong_tien, thanhToan, makh, source, tong_so_bao) VALUES (?, ?, ?, ?, ?, 'WINFORM', ?)",
                        sohd, ngayLap, tongTien, thanhToan, makh, tongSoBao);
                } catch (Exception e) {
                    log.error("Lỗi khi insert m_hoa_don: {}", e.getMessage());
                }
            }

            // Lấy userId để insert web_orders
            Integer userId = null;
            try {
                userId = jdbcB.queryForObject("SELECT id FROM web_users WHERE username = ?", Integer.class, username);
            } catch (Exception e) {}

            if (userId != null) {
                int soOrder = 2 + random.nextInt(5);
                for (int j = 0; j < soOrder; j++) {
                    String orderCode = "WEB" + makh + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                    LocalDate tuNgay = LocalDate.now().minusDays(random.nextInt(30));
                    LocalDate denNgay = tuNgay.plusMonths(1);
                    LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24));
                    
                    String payStat = random.nextBoolean() ? "PAID" : "UNPAID";
                    String delivStat = "PENDING";
                    int r = random.nextInt(3);
                    if (r == 1) delivStat = "APPROVED";
                    else if (r == 2) delivStat = "DELIVERED";
                    
                    jdbcB.update("INSERT INTO web_orders (order_code, user_id, makh, tu_ngay, den_ngay, ghi_chu, sync_status, created_at, payment_status, delivery_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        orderCode, userId, makh, tuNgay, denNgay, "Đơn hàng tự động", "SYNCED", createdAt, payStat, delivStat);
                        
                    Integer orderId = jdbcB.queryForObject("SELECT id FROM web_orders WHERE order_code = ?", Integer.class, orderCode);
                    
                    // Chi tiết đơn hàng (lấy ngẫu nhiên từ DB thực)
                    int soMon = 1 + random.nextInt(listBao.size());
                    for (int k = 0; k < soMon; k++) {
                        var bao = listBao.get(k); // Lấy k báo đầu hoặc ngẫu nhiên (ở đây lấy tuần tự để không bị trùng ma_bao trong 1 đơn)
                        String maBao = (String) bao.get("maBao");
                        String tenBao = (String) bao.get("ten");
                        BigDecimal donGia = new BigDecimal(bao.get("donGia").toString());
                        int soLuong = 50 + random.nextInt(100);
                        BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(soLuong));
                        
                        jdbcB.update("INSERT INTO web_order_items (order_id, ma_bao, ten_bao, don_gia, so_luong, thanh_tien) VALUES (?, ?, ?, ?, ?, ?)",
                            orderId, maBao, tenBao, donGia, soLuong, thanhTien);
                    }
                }
            }
        }
        log.info("Đã tạo xong dữ liệu giả lập đồng bộ với danh sách báo!");
    }
}
