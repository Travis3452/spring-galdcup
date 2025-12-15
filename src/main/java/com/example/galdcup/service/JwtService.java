package com.example.galdcup.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    @Value("${jwt.refresh-expiration-days:7}")
    @Getter
    private int refreshExpDays;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Object principal) {
        return Jwts.builder()
                .setSubject(principal.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Object principal) {
        long refreshMillis = System.currentTimeMillis() + (refreshExpDays * 24L * 60L * 60L * 1000L);
        return Jwts.builder()
                .setSubject(principal.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(refreshMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256) // 최신 방식
                .compact();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey) // parserBuilder 사용
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public int getRefreshTokenMaxAgeSeconds() {
        return refreshExpDays * 24 * 60 * 60;
    }
}