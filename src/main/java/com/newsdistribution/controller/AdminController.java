package com.newsdistribution.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JdbcTemplate jdbcB;

    public AdminController(@Qualifier("jdbcB") JdbcTemplate jdbcB) {
        this.jdbcB = jdbcB;
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<?> getTableData(@PathVariable String tableName) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid table name"));
        }

        try {
            // Lấy từ Website DB
            String sql = "SELECT TOP 100 * FROM " + tableName + " ORDER BY id DESC";
            List<Map<String, Object>> data = jdbcB.queryForList(sql);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy dữ liệu bảng: " + e.getMessage()));
        }
    }

    @GetMapping("/revenue/daily")
    public ResponseEntity<?> getDailyRevenue() {
        try {
            String sql = "SELECT CONVERT(date, o.created_at) as label, SUM(i.thanh_tien) as value " +
                         "FROM web_orders o " +
                         "JOIN web_order_items i ON o.id = i.order_id " +
                         "GROUP BY CONVERT(date, o.created_at) " +
                         "ORDER BY label ASC";
            return ResponseEntity.ok(jdbcB.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<?> getMonthlyRevenue() {
        try {
            String sql = "SELECT FORMAT(o.created_at, 'yyyy-MM') as label, SUM(i.thanh_tien) as value " +
                         "FROM web_orders o " +
                         "JOIN web_order_items i ON o.id = i.order_id " +
                         "GROUP BY FORMAT(o.created_at, 'yyyy-MM') " +
                         "ORDER BY label ASC";
            return ResponseEntity.ok(jdbcB.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/revenue/yearly")
    public ResponseEntity<?> getYearlyRevenue() {
        try {
            String sql = "SELECT FORMAT(o.created_at, 'yyyy') as label, SUM(i.thanh_tien) as value " +
                         "FROM web_orders o " +
                         "JOIN web_order_items i ON o.id = i.order_id " +
                         "GROUP BY FORMAT(o.created_at, 'yyyy') " +
                         "ORDER BY label ASC";
            return ResponseEntity.ok(jdbcB.queryForList(sql));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
