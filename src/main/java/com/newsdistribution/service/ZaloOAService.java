package com.newsdistribution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ZaloOAService {
    
    private static final Logger logger = LoggerFactory.getLogger(ZaloOAService.class);

    /**
     * Mô phỏng gửi tin nhắn Zalo OA
     * Để đăng ký Zalo OA miễn phí:
     * 1. Truy cập oa.zalo.me
     * 2. Tạo Official Account loại "Doanh nghiệp" hoặc "Cơ quan nhà nước"
     * 3. Nạp tiền/Xác thực để dùng Zalo Notification Service (ZNS)
     * 4. Tạo template tin nhắn và gọi API thực tế tại đây.
     */
    public void sendZaloMessage(String phoneNumber, String message) {
        logger.info("================ ZALO OA MOCK ================");
        logger.info("Sending Zalo message to: {}", phoneNumber);
        logger.info("Content: {}", message);
        logger.info("==============================================");
    }
}
