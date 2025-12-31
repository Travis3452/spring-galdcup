package com.example.galdcup.service;

import com.example.galdcup.client.GoogleOAuthClient;
import com.example.galdcup.dto.auth.AuthDto;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.UserRepository;
import com.example.galdcup.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuthClient googleClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Transactional
    public AuthDto handleGoogleCallback(String code) {
        var tokens = googleClient.exchangeCodeForToken(code, googleRedirectUri);
        var profile = googleClient.fetchUserProfile(tokens.accessToken());

        User user = userRepository.findByOauthId(profile.sub())
                .orElseGet(() -> userRepository.save(User.builder()
                        .oauthId(profile.sub())
                        .email(profile.email())
                        .nickname(profile.name())
                        .role(User.Role.USER)
                        .build()));

        return createTokens(user);
    }

    // 로그인/갱신 시 토큰 발급
    @Transactional
    public AuthDto createTokens(User user) {
        redisTemplate.delete("refreshToken:" + user.getId());

        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        redisTemplate.opsForValue().set(
                "refreshToken:" + user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenMaxAgeSeconds(),
                TimeUnit.SECONDS
        );

        // AccessToken 생성 시 User의 Role 반영
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                Collections.singletonList(user.getRole().name())
        );

        return AuthDto.of(accessToken, refreshToken, jwtTokenProvider.getRefreshTokenMaxAgeSeconds(), user.getNickname());
    }

    @Transactional
    public AuthDto refreshTokens(String refreshToken) {
        var claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());

        String storedToken = redisTemplate.opsForValue().get("refreshToken:" + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return createTokens(user);
    }

    @Transactional
    public void deleteRefreshTokens(Long userId) {
        redisTemplate.delete("refreshToken:" + userId);
    }
}