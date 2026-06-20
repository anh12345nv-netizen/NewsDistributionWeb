package com.newsdistribution.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    private final JdbcTemplate jdbc;
    private final JdbcTemplate jdbcC;

    public AnalyticsController(@Qualifier("jdbcB") JdbcTemplate jdbc, @Qualifier("jdbcC") JdbcTemplate jdbcC) {
        this.jdbc = jdbc;
        this.jdbcC = jdbcC;
    }

    @GetMapping("/heatmap")
    public ResponseEntity<?> getHeatmapData(
            @RequestParam String page,
            @RequestParam String from,
            @RequestParam String to) {
        try {
            String startDateTime = from + " 00:00:00";
            String endDateTime = to + " 23:59:59";
            
            List<Map<String, Object>> data = jdbc.queryForList("""
                SELECT element_x, element_y, element_id, COUNT(*) as count
                FROM web_activity_log
                WHERE page = ? AND created_at >= ? AND created_at <= ?
                  AND element_x IS NOT NULL AND element_y IS NOT NULL
                GROUP BY element_x, element_y, element_id
                """,
                page, startDateTime, endDateTime
            );
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sync-logs")
    public ResponseEntity<?> getSyncLogs() {
        try {
            List<Map<String, Object>> logs = jdbcC.queryForList(
                "SELECT TOP 20 id, sync_type, table_name, rows_synced, status, error_msg, synced_at FROM sync_log ORDER BY synced_at DESC"
            );
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Integer totalUsers = jdbc.queryForObject("SELECT COUNT(*) FROM web_users WHERE role = 'AGENCY'", Integer.class);
            Integer totalOrders = jdbc.queryForObject("SELECT COUNT(*) FROM web_orders", Integer.class);
            Integer totalBao = jdbcC.queryForObject("SELECT COUNT(*) FROM m_bao", Integer.class);
            Integer totalLogs = jdbc.queryForObject("SELECT COUNT(*) FROM web_activity_log", Integer.class);
            
            return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers != null ? totalUsers : 0,
                "totalOrders", totalOrders != null ? totalOrders : 0,
                "totalPublications", totalBao != null ? totalBao : 0,
                "totalActivityLogs", totalLogs != null ? totalLogs : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
