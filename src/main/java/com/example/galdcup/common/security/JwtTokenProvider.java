package com.example.galdcup.common.security;

import com.example.galdcup.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_USE = "token_use";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long expirationMillis;
    private final int refreshExpDays;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expirationMillis,
                            @Value("${jwt.refresh-expiration-days}") int refreshExpDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
        this.refreshExpDays = refreshExpDays;
    }

    /**
     * AccessToken 생성
     */
    public String createAccessToken(Long userId, List<String> roles) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim(TOKEN_USE, ACCESS)
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
                .claim(TOKEN_USE, REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(refreshMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * AccessToken의 서명, 만료, 용도 및 권한을 검증하고 Authentication 생성
     */
    public Authentication getAccessAuthentication(String token) {
        Claims claims = parseClaims(token, ACCESS);
        Long userId = getUserId(claims);

        List<?> roles = claims.get("roles", List.class);
        if (roles == null || roles.isEmpty() || !(roles.get(0) instanceof String roleName)) {
            throw new JwtException("유효한 권한 정보가 없는 액세스 토큰입니다.");
        }

        User.Role role;
        try {
            role = User.Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new JwtException("유효하지 않은 액세스 토큰 권한입니다.", e);
        }

        User dummyUser = User.builder()
                .id(userId)
                .role(role)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(dummyUser);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    /**
     * 용도 정보가 없는 기존 토큰도 거부하여 토큰 혼용 방지
     */
    private Claims parseClaims(String token, String expectedUse) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!expectedUse.equals(claims.get(TOKEN_USE, String.class))) {
            throw new JwtException("잘못된 토큰 용도입니다.");
        }
        if (claims.getExpiration() == null) {
            throw new JwtException("만료 정보가 없는 토큰입니다.");
        }

        return claims;
    }

    /**
     * RefreshToken만 허용하며 인증 실패는 401 처리 대상 예외로 변환
     */
    public Long getUserIdFromRefreshToken(String token) {
        try {
            return getUserId(parseClaims(token, REFRESH));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("유효하지 않은 리프레시 토큰입니다.", e);
        }
    }

    private Long getUserId(Claims claims) {
        try {
            long userId = Long.parseLong(claims.getSubject());
            if (userId <= 0) {
                throw new JwtException("유효하지 않은 사용자 식별자입니다.");
            }
            return userId;
        } catch (NumberFormatException e) {
            throw new JwtException("유효하지 않은 사용자 식별자입니다.", e);
        }
    }

    /**
     * RefreshToken 유효기간 반환
     */
    public int getRefreshTokenTTLSeconds() {
        return refreshExpDays * 24 * 60 * 60;
    }
}
