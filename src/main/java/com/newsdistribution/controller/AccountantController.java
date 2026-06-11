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

    public AccountantController(@Qualifier("jdbcC") JdbcTemplate jdbcC) {
        this.jdbcC = jdbcC;
    }

    @GetMapping("/tables/{tableName}")
    public ResponseEntity<?> getTableData(@PathVariable String tableName) {
        // Prevent SQL Injection basic
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid table name"));
        }

        try {
            // Using TOP 100 to avoid overwhelming the frontend or DB
            String sql = "SELECT TOP 100 * FROM " + tableName;
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
                    String sql = "SELECT COUNT(*) FROM " + table;
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
}
