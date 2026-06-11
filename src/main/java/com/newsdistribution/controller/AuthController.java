package com.newsdistribution.controller;

import com.newsdistribution.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(authService.login(body.get("username"), body.get("password")));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(authService.register(
                body.get("username"),
                body.get("password"),
                body.get("tenHienThi"),
                body.get("email"),
                body.get("tenDoanhNghiep"),
                body.get("maSoThue"),
                body.get("diaChi"),
                body.get("soDienThoai")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register-accountant")
    public ResponseEntity<?> registerAccountant(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(authService.registerAccountant(
                body.get("username"),
                body.get("password"),
                body.get("tenHienThi"),
                body.get("email")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
