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
            String sql;
            if (tableName.equalsIgnoreCase("m_hoa_don")) {
                sql = "SELECT TOP 500 * FROM m_hoa_don WITH (NOLOCK) ORDER BY ngayLapPhieu DESC, sohd DESC";
            } else if (tableName.equalsIgnoreCase("m_ton_kho")) {
                sql = "SELECT TOP 1000 * FROM m_ton_kho WITH (NOLOCK) ORDER BY ngay DESC, maBao ASC";
            } else if (tableName.equalsIgnoreCase("web_orders")) {
                sql = "SELECT TOP 500 * FROM web_orders WITH (NOLOCK) ORDER BY created_at DESC";
            } else {
                sql = "SELECT * FROM " + tableName + " WITH (NOLOCK)";
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
            // Only sum paid invoices (thanhToan = 1)
            String sql = "SELECT source, SUM(tong_tien) as revenue, COUNT(DISTINCT sohd) as count FROM m_hoa_don WHERE source IS NOT NULL AND thanhToan = 1 GROUP BY source";
            List<Map<String, Object>> rows = jdbcC.queryForList(sql);
            Map<String, Map<String, Object>> revenue = new HashMap<>();
            
            Map<String, Object> webData = new HashMap<>();
            webData.put("revenue", 0.0);
            webData.put("count", 0);
            revenue.put("WEB", webData);
            
            Map<String, Object> winformData = new HashMap<>();
            winformData.put("revenue", 0.0);
            winformData.put("count", 0);
            revenue.put("WINFORM", winformData);
            
            for (Map<String, Object> row : rows) {
                String source = (String) row.get("source");
                Number rev = (Number) row.get("revenue");
                Number count = (Number) row.get("count");
                if (source != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("revenue", rev != null ? rev.doubleValue() : 0.0);
                    data.put("count", count != null ? count.intValue() : 0);
                    revenue.put(source.trim().toUpperCase(), data);
                }
            }
            return ResponseEntity.ok(revenue);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/stats/monthly-revenue")
    public ResponseEntity<?> getMonthlyRevenue() {
        try {
            // Only sum paid invoices (thanhToan = 1)
            String sql = "SELECT source, MONTH(ngayLapPhieu) as month, SUM(tong_tien) as revenue " +
                         "FROM m_hoa_don WHERE source IS NOT NULL AND thanhToan = 1 GROUP BY source, MONTH(ngayLapPhieu)";
            List<Map<String, Object>> rows = jdbcC.queryForList(sql);
            
            Map<String, double[]> monthly = new HashMap<>();
            monthly.put("WEB", new double[12]);
            monthly.put("WINFORM", new double[12]);
            
            for (Map<String, Object> row : rows) {
                String source = (String) row.get("source");
                Number month = (Number) row.get("month");
                Number rev = (Number) row.get("revenue");
                if (source != null && month != null && rev != null) {
                    String src = source.trim().toUpperCase();
                    if (monthly.containsKey(src)) {
                        monthly.get(src)[month.intValue() - 1] = rev.doubleValue();
                    }
                }
            }
            return ResponseEntity.ok(monthly);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/invoices/{sohd}/details")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable String sohd) {
        try {
            // Xác định xem hóa đơn này thuộc WINFORM hay WEB từ DB C
            String sourceSql = "SELECT TOP 1 source FROM m_hoa_don WHERE sohd = ?";
            String source = jdbcC.queryForObject(sourceSql, String.class, sohd);

            if (source != null && "WEB".equalsIgnoreCase(source.trim())) {
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
