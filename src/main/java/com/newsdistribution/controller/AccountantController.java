package com.newsdistribution.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accountant")
public class AccountantController {

    private final JdbcTemplate jdbcC;
    private final JdbcTemplate jdbcB;

    public AccountantController(@Qualifier("jdbcC") JdbcTemplate jdbcC, @Qualifier("jdbcB") JdbcTemplate jdbcB) {
        this.jdbcC = jdbcC;
        this.jdbcB = jdbcB;
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<?> getTableData(@PathVariable String tableName) {
        // Prevent SQL Injection basic
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid table name"));
        }

        try {
            // Retrieve all records for Accountant Audit
            String sql = "SELECT * FROM " + tableName;
            if (tableName.equalsIgnoreCase("m_hoa_don")) {
                sql += " ORDER BY ngayLapPhieu DESC, sohd DESC";
            } else if (tableName.equalsIgnoreCase("web_orders")) {
                sql += " ORDER BY created_at DESC";
            }
            List<Map<String, Object>> data = jdbcC.queryForList(sql);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error querying table: " + e.getMessage()));
        }
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<?> getStatsOverview() {
        try {
            List<String> tables = List.of("m_bao", "m_hoa_don", "m_khach_hang", "m_ton_kho", "sync_log");
            Map<String, Integer> stats = new HashMap<>();

            for (String table : tables) {
                try {
                    String sql;
                    if ("m_hoa_don".equals(table)) {
                        sql = "SELECT COUNT(DISTINCT sohd) FROM " + table;
                    } else {
                        sql = "SELECT COUNT(*) FROM " + table;
                    }
                    Integer count = jdbcC.queryForObject(sql, Integer.class);
                    stats.put(table, count != null ? count : 0);
                } catch (Exception e) {
                    stats.put(table, 0);
                }
            }
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats/revenue")
    public ResponseEntity<?> getRevenueStats() {
        try {
            String sql = "SELECT source, SUM(tong_tien) as revenue FROM m_hoa_don WHERE source IS NOT NULL GROUP BY source";
            List<Map<String, Object>> rows = jdbcC.queryForList(sql);
            Map<String, Double> revenue = new HashMap<>();
            revenue.put("WEB", 0.0);
            revenue.put("WINFORM", 0.0);
            
            for (Map<String, Object> row : rows) {
                String source = (String) row.get("source");
                Number rev = (Number) row.get("revenue");
                if (source != null && rev != null) {
                    revenue.put(source.toUpperCase(), rev.doubleValue());
                }
            }
            return ResponseEntity.ok(revenue);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "cause", e.getCause() != null ? e.getCause().getMessage() : ""));
        }
    }

    @GetMapping("/invoices/{sohd}/details")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable String sohd) {
        try {
            // Xác định xem hóa đơn này thuộc WINFORM hay WEB từ DB C
            String sourceSql = "SELECT TOP 1 source FROM m_hoa_don WHERE sohd = ?";
            String source = jdbcC.queryForObject(sourceSql, String.class, sohd);

            if ("WEB".equalsIgnoreCase(source)) {
                // Lấy chi tiết đơn hàng Web từ DB B
                String sql = "SELECT i.* FROM web_order_items i JOIN web_orders o ON i.order_id = o.id WHERE o.order_code = ?";
                List<Map<String, Object>> details = jdbcB.queryForList(sql, sohd);
                return ResponseEntity.ok(details);
            } else {
                // Hiện tại chưa có bảng chi tiết hóa đơn cho WINFORM
                return ResponseEntity.ok(List.of()); // Trả về list rỗng để frontend hiển thị thông báo
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
