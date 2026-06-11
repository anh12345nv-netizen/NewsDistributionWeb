package com.newsdistribution.controller;

import com.newsdistribution.entity.WebOrder;
import com.newsdistribution.entity.WebUser;
import com.newsdistribution.repository.WebUserRepository;
import com.newsdistribution.service.AgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/agency")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService agencyService;
    private final WebUserRepository userRepo;

    private String getMakh(Authentication auth) {
        return (String) auth.getDetails();
    }

    private Integer getUserId(Authentication auth) {
        if (auth == null) return null;
        String username = auth.getName();
        return userRepo.findByUsername(username)
                .map(WebUser::getId)
                .orElse(null);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        return ResponseEntity.ok(agencyService.getDashboard(getMakh(auth)));
    }

    @GetMapping("/publications")
    public ResponseEntity<?> publications() {
        return ResponseEntity.ok(agencyService.getPublications());
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(Authentication auth) {
        return ResponseEntity.ok(Map.of("orders", agencyService.getOrdersByMakh(getMakh(auth))));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            var order = agencyService.createOrder(getMakh(auth), getUserId(auth), body);
            return ResponseEntity.ok(Map.of("orderCode", order.getOrderCode(), "id", order.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
