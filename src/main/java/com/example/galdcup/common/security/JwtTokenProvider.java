package com.example.galdcup.common.security;

import com.example.galdcup.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    @Value("${jwt.refresh-expiration-days}")
    private int refreshExpDays;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * AccessToken 생성
     */
    public String createAccessToken(Long userId, List<String> roles) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * RefreshToken 생성
     */
    public String createRefreshToken(Long userId) {
        long refreshMillis = System.currentTimeMillis() + (refreshExpDays * 24L * 60L * 60L * 1000L);
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(refreshMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 유효성 검사
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 Authentication 추출
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        Long userId = Long.parseLong(claims.getSubject());

        List<String> roles = claims.get("roles", List.class);

        String roleName = (roles != null && !roles.isEmpty()) ? roles.get(0) : User.Role.USER.name();

        User dummyUser = User.builder()
                .id(userId)
                .role(User.Role.valueOf(roleName))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(dummyUser);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    /**
     * Claims 파싱
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserIdFromToken(String token) {
        try {
            return Long.parseLong(parseClaims(token).getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }

    /**
     * RefreshToken 유효기간 반환
     */
    public int getRefreshTokenTTLSeconds() {
        return refreshExpDays * 24 * 60 * 60;
    }
}