package com.newsdistribution.service;

import com.newsdistribution.entity.WebUser;
import com.newsdistribution.repository.WebUserRepository;
import com.newsdistribution.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final WebUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String password) {
        WebUser user = userRepo.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Sai tên đăng nhập hoặc mật khẩu"));

        if (user.getIsActive() != null && !user.getIsActive())
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");

        if (!passwordEncoder.matches(password, user.getPasswordHash()))
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu");

        String accessToken = jwtUtil.generateAccessToken(user);

        return Map.of(
            "accessToken", accessToken,
            "role", user.getRole(),
            "makh", user.getMakh() != null ? user.getMakh() : "",
            "tenHienThi", user.getTenHienThi() != null ? user.getTenHienThi() : username
        );
    }

    public Map<String, Object> register(String username, String password, String tenHienThi, String email, 
                                        String tenDoanhNghiep, String maSoThue, String diaChi, String soDienThoai) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trong hệ thống");
        }

        String maxMakh = userRepo.findMaxMakh();
        String autoMakh;
        if (maxMakh == null || maxMakh.length() < 3) {
            autoMakh = "DL001";
        } else {
            try {
                int nextId = Integer.parseInt(maxMakh.substring(2)) + 1;
                autoMakh = String.format("DL%03d", nextId);
            } catch (Exception e) {
                autoMakh = "DL" + java.util.UUID.randomUUID().toString().substring(0, 3).toUpperCase();
            }
        }

        WebUser user = WebUser.builder()
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .role("AGENCY")
            .makh(autoMakh)
            .tenHienThi(tenHienThi != null && !tenHienThi.isBlank() ? tenHienThi : username)
            .email(email)
            .tenDoanhNghiep(tenDoanhNghiep)
            .maSoThue(maSoThue)
            .diaChi(diaChi)
            .soDienThoai(soDienThoai)
            .isActive(true)
            .build();

        userRepo.save(user);

        return Map.of("success", true, "message", "Đăng ký tài khoản đại lý thành công");
    }

    public Map<String, Object> registerAccountant(String username, String password, String tenHienThi, String email) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trong hệ thống");
        }

        WebUser user = WebUser.builder()
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .role("ACCOUNTANT")
            .tenHienThi(tenHienThi != null && !tenHienThi.isBlank() ? tenHienThi : username)
            .email(email)
            .isActive(true)
            .build();

        userRepo.save(user);

        return Map.of("success", true, "message", "Đăng ký tài khoản Kế toán thành công");
    }
}
