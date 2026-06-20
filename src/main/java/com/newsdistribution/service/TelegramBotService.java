package com.newsdistribution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class TelegramBotService {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);
    private final HttpClient httpClient;

    @Value("${telegram.bot.token:}")
    private String botToken;

    public TelegramBotService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Gửi tin nhắn qua Telegram Bot
     * Hướng dẫn thiết lập:
     * 1. Lên Telegram tìm @BotFather, gõ /newbot để tạo Bot và lấy TOKEN.
     * 2. Tìm bot vừa tạo, ấn Start để chat với nó.
     * 3. Lấy Chat ID của bạn bằng cách truy cập: https://api.telegram.org/bot<TOKEN>/getUpdates
     * 4. Thêm telegram.bot.token vào application.yml
     */
    public void sendMessage(String chatId, String message) {
        if (botToken == null || botToken.isBlank()) {
            logger.warn("Telegram Bot Token chưa được cấu hình. Chỉ mô phỏng (Mock) gửi tin nhắn.");
            logger.info("================ TELEGRAM MOCK ================");
            logger.info("Chat ID: {}", chatId);
            logger.info("Nội dung: {}", message);
            logger.info("===============================================");
            return;
        }

        try {
            String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            
            // Xây dựng JSON payload thủ công đơn giản
            String jsonPayload = String.format("{\"chat_id\":\"%s\",\"text\":\"%s\",\"parse_mode\":\"HTML\"}", 
                                               chatId, 
                                               message.replace("\"", "\\\"").replace("\n", "\\n"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("Đã gửi tin nhắn Telegram thành công tới Chat ID: {}", chatId);
            } else {
                logger.error("Lỗi khi gửi Telegram. HTTP Code: {}. Response: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Ngoại lệ khi gọi Telegram API: {}", e.getMessage());
        }
    }
}
