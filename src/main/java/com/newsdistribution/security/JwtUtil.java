package com.newsdistribution.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.newsdistribution.entity.WebUser;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry-ms}")
    private long accessExpiry;

    public String generateAccessToken(WebUser user) {
        return Jwts.builder()
            .setSubject(user.getUsername())
            .claim("role", user.getRole())
            .claim("makh", user.getMakh())
            .claim("userId", user.getId())
            .claim("tenHienThi", user.getTenHienThi())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessExpiry))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
            .build().parseClaimsJws(token).getBody();
    }

    public String extractUsername(String token) { return extractClaims(token).getSubject(); }
    public String extractRole(String token) { return extractClaims(token).get("role", String.class); }
    public String extractMakh(String token) { return extractClaims(token).get("makh", String.class); }

    public boolean isTokenValid(String token) {
        try { return !extractClaims(token).getExpiration().before(new Date()); }
        catch (Exception e) { return false; }
    }
}
