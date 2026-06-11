package com.newsdistribution.controller;

import com.newsdistribution.service.AiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final JdbcTemplate jdbcC;

    public AiController(AiService aiService, @Qualifier("jdbcC") JdbcTemplate jdbcC) {
        this.aiService = aiService;
        this.jdbcC = jdbcC;
    }

    @GetMapping("/forecast/{maBao}")
    public ResponseEntity<?> forecast(@PathVariable String maBao) {
        try {
            return ResponseEntity.ok(aiService.forecast(maBao));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/publications")
    public ResponseEntity<?> listPublications() {
        return ResponseEntity.ok(
            jdbcC.queryForList("SELECT DISTINCT maBao, tenBao FROM m_ton_kho ORDER BY tenBao")
        );
    }
}
