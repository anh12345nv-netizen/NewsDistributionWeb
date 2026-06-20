package com.newsdistribution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public void sendOrderNotification(String toEmail, String makh, String orderCode, String orderType, String totalMoney) {
        if (javaMailSender == null || senderEmail == null || senderEmail.isBlank()) {
            logger.warn("Chưa cấu hình Email Server (spring.mail.username). Bỏ qua gửi Email thực tế.");
            logger.info("================ EMAIL MOCK ================");
            logger.info("To: {}", toEmail);
            logger.info("Subject: CÓ ĐƠN ĐẶT BÁO MỚI ({})", orderCode);
            logger.info("Content: Đại lý {} vừa đặt đơn {} ({}) với tổng tiền {} VNĐ.", makh, orderCode, orderType, totalMoney);
            logger.info("============================================");
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "Hệ Thống Phân Phối Báo");
            helper.setTo(toEmail);
            helper.setSubject("🚨 CÓ ĐƠN ĐẶT BÁO MỚI - " + orderCode);

            String htmlContent = String.format(
                "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 8px;'>" +
                "<h2 style='color: #0056b3;'>CÓ ĐƠN ĐẶT BÁO MỚI</h2>" +
                "<p>Hệ thống vừa ghi nhận một đơn đặt hàng mới từ đại lý.</p>" +
                "<ul>" +
                "<li><b>Mã đại lý:</b> %s</li>" +
                "<li><b>Mã đơn hàng:</b> %s</li>" +
                "<li><b>Loại đơn:</b> %s</li>" +
                "<li><b>Tổng tiền:</b> <span style='color: red; font-weight: bold;'>%s VNĐ</span></li>" +
                "</ul>" +
                "<p>Vui lòng đăng nhập vào trang Admin để kiểm tra và duyệt đơn.</p>" +
                "</div>",
                makh, orderCode, orderType, totalMoney
            );

            helper.setText(htmlContent, true); // true = isHtml

            javaMailSender.send(message);
            logger.info("Đã gửi Email thông báo đơn hàng thành công tới: {}", toEmail);

        } catch (Exception e) {
            logger.error("Lỗi khi gửi Email thông báo: {}", e.getMessage());
        }
    }
}
