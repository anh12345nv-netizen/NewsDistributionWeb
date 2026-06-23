package com.newsdistribution.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Service
public class AiService {

    private final JdbcTemplate jdbcB;
    private final JdbcTemplate jdbcC;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    public AiService(@Qualifier("jdbcB") JdbcTemplate jdbcB, @Qualifier("jdbcC") JdbcTemplate jdbcC) {
        this.jdbcB = jdbcB;
        this.jdbcC = jdbcC;
    }

    public Map<String, Object> forecast(String maBao) {
        // Lấy 90 ngày gần nhất
        var rows = jdbcC.queryForList("""
            SELECT TOP 90 ngay, ISNULL(slPhatHanh,0) as y
            FROM m_ton_kho
            WHERE maBao=?
            ORDER BY ngay ASC
            """, maBao);

        if (rows.isEmpty()) throw new RuntimeException("Không có dữ liệu cho báo: " + maBao);

        String tenBao = jdbcC.queryForObject("SELECT ten FROM m_bao WHERE maBao=?", String.class, maBao);

        int n = rows.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        List<Map<String,Object>> scatterData = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = ((Number) rows.get(i).get("y")).doubleValue();
            sumX += x; sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            Map<String, Object> point = new HashMap<>();
            point.put("dayIndex", i);
            point.put("actual", y);
            point.put("date", rows.get(i).get("ngay").toString());
            scatterData.add(point);
        }

        // Linear regression: y = slope*x + intercept
        double denominator = n * sumX2 - sumX * sumX;
        double slope = (denominator == 0) ? 0.0 : (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        // R²
        double yMean = sumY / n;
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            double y = ((Number) rows.get(i).get("y")).doubleValue();
            double yPred = slope * i + intercept;
            ssTot += Math.pow(y - yMean, 2);
            ssRes += Math.pow(y - yPred, 2);
            scatterData.get(i).put("predicted", Math.max(0, Math.round(yPred)));
        }
        double rSquared = 1 - (ssRes / (ssTot == 0 ? 1 : ssTot));

        double forecastNext = Math.max(0, Math.round(slope * n + intercept));

        return Map.of(
            "maBao", maBao,
            "tenBao", tenBao != null ? tenBao : maBao,
            "coefficients", Map.of("slope", Math.round(slope * 100.0) / 100.0, "intercept", Math.round(intercept)),
            "rSquared", Math.round(rSquared * 1000.0) / 1000.0,
            "forecastNext", (long) forecastNext,
            "scatterData", scatterData
        );
    }

    public String chatVirtualAssistant(String message, String role, String makh) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            return "Tính năng Trợ lý ảo AI chưa được cấu hình API Key. Vui lòng thiết lập `ai.gemini.api-key` trong hệ thống để kích hoạt.";
        }
        
        try {
            // Build context data
            Map<String, Object> contextData = new HashMap<>();
            contextData.put("role", role);
            
            if ("ACCOUNTANT".equals(role)) {
                // Get basic system stats
                contextData.put("totalWebRevenue", jdbcC.queryForObject("SELECT SUM(tong_tien) FROM m_hoa_don WHERE source = 'WEB' AND thanhToan = 1", Double.class));
                contextData.put("totalWinformRevenue", jdbcC.queryForObject("SELECT SUM(tong_tien) FROM m_hoa_don WHERE source = 'WINFORM' AND thanhToan = 1", Double.class));
            } else if ("AGENCY".equals(role)) {
                // Get agency specific stats
                contextData.put("makh", makh);
                contextData.put("totalOrders", jdbcB.queryForObject("SELECT COUNT(*) FROM web_orders WHERE makh = ?", Integer.class, makh));
                contextData.put("unpaidAmount", jdbcB.queryForObject("SELECT ISNULL(SUM(i.thanh_tien),0) FROM web_orders o LEFT JOIN web_order_items i ON o.id = i.order_id WHERE o.makh = ? AND (o.payment_status = 'UNPAID' OR o.payment_status IS NULL)", Double.class, makh));
            }
            
            String contextStr = mapper.writeValueAsString(contextData);
            String systemPrompt = "Bạn là Trợ lí thông minh, hỗ trợ nội bộ cho hệ thống phân phối báo chí News Distribution. " +
                "Trả lời bằng tiếng Việt, ngắn gọn, tự nhiên như một nhân viên hỗ trợ chính hãng. " +
                "QUAN TRỌNG: Tuyệt đối không dùng bất kỳ kí hiệu định dạng Markdown nào như **, *, #, -, __. Chỉ viết văn bản thuần tú. " +
                "Dưới đây là dữ liệu thống kê người dùng hiện tại (JSON). " +
                "Hãy dùng dữ liệu này nếu câu hỏi có liên quan. Dữ liệu ngữ cảnh: " + contextStr + "\\n\\nCâu hỏi của người dùng: " + message;
                
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + geminiApiKey;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", systemPrompt)))
            ));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            
            // Parse response
            Map<String, Object> resMap = mapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) resMap.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            
            return "Xin lỗi, tôi không thể xử lý câu trả lời lúc này.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Hệ thống AI đang gặp sự cố: " + e.getMessage();
        }
    }
}
