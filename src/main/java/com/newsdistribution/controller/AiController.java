package com.newsdistribution.controller;

import com.newsdistribution.service.AiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final JdbcTemplate jdbcC;
    private final JdbcTemplate jdbcB;

    public AiController(AiService aiService, @Qualifier("jdbcC") JdbcTemplate jdbcC, @Qualifier("jdbcB") JdbcTemplate jdbcB) {
        this.aiService = aiService;
        this.jdbcC = jdbcC;
        this.jdbcB = jdbcB;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            String role = "UNKNOWN";
            if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ACCOUNTANT"))) {
                role = "ACCOUNTANT";
            } else if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_AGENCY"))) {
                role = "AGENCY";
            }
            
            String makh = null;
            if ("AGENCY".equals(role)) {
                try {
                    makh = jdbcB.queryForObject("SELECT makh FROM web_users WHERE username = ?", String.class, auth.getName());
                } catch (Exception ignored) {}
            }
            
            String response = aiService.chatVirtualAssistant(body.get("message"), role, makh);
            return ResponseEntity.ok(Map.of("reply", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
