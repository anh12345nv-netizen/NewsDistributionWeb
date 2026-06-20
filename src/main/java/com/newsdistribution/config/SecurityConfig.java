package com.newsdistribution.config;

import com.newsdistribution.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints for API
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/ws/**", "/ws/info/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Allow static files publicly
                .requestMatchers("/", "/index.html", "/login.html", "/register.html", "/favicon.ico", "/js/**", "/css/**", "/images/**", "/admin/**", "/agency/**", "/accountant/**", "/uploads/**").permitAll()
                
                // API roles
                .requestMatchers("/api/agency/**").hasRole("AGENCY")
                .requestMatchers("/api/admin/**").hasRole("ADMIN_WEB")
                .requestMatchers("/api/accountant/**").hasRole("ACCOUNTANT")
                .requestMatchers("/api/ai/**").hasAnyRole("AGENCY", "ADMIN_WEB", "ACCOUNTANT")
                .requestMatchers("/api/activity/**").permitAll() // Clicks activity logging doesn't block
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
