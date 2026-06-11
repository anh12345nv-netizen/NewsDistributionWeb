package com.newsdistribution.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AiService {

    private final JdbcTemplate jdbcC;

    public AiService(@Qualifier("jdbcC") JdbcTemplate jdbcC) {
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
}
