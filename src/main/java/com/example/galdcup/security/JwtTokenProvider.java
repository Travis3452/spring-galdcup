package com.example.galdcup.security;

import com.example.galdcup.entity.User;
import com.example.galdcup.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenProvider {

    private final UserRepository userRepository;
    private final SecretKey secretKey;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    @Value("${jwt.refresh-expiration-days}")
    private int refreshExpDays;

    public JwtTokenProvider(UserRepository userRepository, @Value("${jwt.secret}") String secret) {
        this.userRepository = userRepository;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * AccessToken 생성
     */
    public String createAccessToken(Long userId, List<String> roles) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * RefreshToken 생성
     */
    public String createRefreshToken(Long userId) {
        long refreshMillis = System.currentTimeMillis() + (refreshExpDays * 24L * 60L * 60L * 1000L);
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(refreshMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    /**
     * Claims 파싱
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * RefreshToken 유효기간 반환
     */
    public int getRefreshTokenMaxAgeSeconds() {
        return refreshExpDays * 24 * 60 * 60;
    }

    /**
     * RefreshToken 파싱
     */
    public Claims parseRefreshToken(String token) {
        return parseClaims(token);
    }
}