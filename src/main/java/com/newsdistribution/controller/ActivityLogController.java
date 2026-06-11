package com.newsdistribution.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.newsdistribution.entity.WebUser;
import com.newsdistribution.repository.WebUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {

    private final JdbcTemplate jdbc;
    private final WebUserRepository userRepo;

    public ActivityLogController(@Qualifier("jdbcB") JdbcTemplate jdbc, WebUserRepository userRepo) {
        this.jdbc = jdbc;
        this.userRepo = userRepo;
    }

    @PostMapping("/log")
    public ResponseEntity<?> logActivity(@RequestBody Map<String, Object> body, Authentication auth, HttpServletRequest request) {
        try {
            Integer userId = null;
            if (auth != null) {
                userId = userRepo.findByUsername(auth.getName()).map(WebUser::getId).orElse(null);
            }
            String ipAddress = request.getRemoteAddr();
            
            jdbc.update("""
                INSERT INTO web_activity_log (user_id, page, action, element_id, element_x, element_y, ip_address)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                body.get("page"),
                body.get("action"),
                body.get("elementId"),
                body.get("x"),
                body.get("y"),
                ipAddress
            );
            return ResponseEntity.ok(Map.of("status", "logged"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
