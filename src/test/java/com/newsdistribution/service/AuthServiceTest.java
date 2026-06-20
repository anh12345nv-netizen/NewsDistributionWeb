package com.newsdistribution.service;

import com.newsdistribution.entity.WebUser;
import com.newsdistribution.repository.WebUserRepository;
import com.newsdistribution.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private WebUserRepository userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    /**
     * Vấn đề 1: Lỗi MDM (Mã KH sinh ngẫu nhiên)
     * Kiểm tra khi đại lý đăng ký, hệ thống tự động sinh ra mã DL + UUID (gây ra DL475014)
     */
    @Test
    void testRegister_GeneratesRandomMakh() {
        when(userRepo.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed_pass");

        authService.register("testuser", "pass", "Test Agency", "test@gmail.com",
            "Công ty ABC", "123456789", "TPHCM", "0901234567");

        ArgumentCaptor<WebUser> userCaptor = ArgumentCaptor.forClass(WebUser.class);
        verify(userRepo).save(userCaptor.capture());

        WebUser savedUser = userCaptor.getValue();
        
        // Assert that the generated makh starts with "DL"
        assertTrue(savedUser.getMakh().startsWith("DL"));
        // Assert that it is 5 characters long (DL + 3 chars)
        assertEquals(5, savedUser.getMakh().length());
        
        // This proves the fix: The system matches the standard WinForm format "DL0xx"
        assertTrue(savedUser.getMakh().matches("^DL\\d{3}$"), 
            "The generated code MUST match the WinForm DL0xx format.");
    }
}
