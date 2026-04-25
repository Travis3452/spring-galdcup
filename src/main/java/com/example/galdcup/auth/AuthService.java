package com.example.galdcup.auth;

import com.example.galdcup.auth.redis.RefreshTokenRedisManager;
import com.example.galdcup.auth.response.AuthDto;
import com.example.galdcup.common.client.GoogleOAuthClient;
import com.example.galdcup.common.security.JwtTokenProvider;
import com.example.galdcup.user.UserService;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuthClient googleClient;
    private final RefreshTokenRedisManager refreshTokenRedisManager;
    private final UserValidator userValidator;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    /**
     * 구글 OAuth 콜백 처리
     */
    public AuthDto handleGoogleCallback(String code) {
        var tokens = googleClient.exchangeCodeForToken(code, googleRedirectUri);
        var profile = googleClient.fetchUserProfile(tokens.accessToken());

        User user = userService.getOrCreateUser(profile.sub(), profile.email());

        return createTokens(user);
    }

    /**
     * 로그인/갱신 시 토큰 발급 및 Redis 저장
     */
    public AuthDto createTokens(User user) {
        String refreshTokenStr = jwtTokenProvider.createRefreshToken(user.getId());
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                Collections.singletonList(user.getRole().name())
        );

        long ttlSeconds = jwtTokenProvider.getRefreshTokenTTLSeconds();
        refreshTokenRedisManager.saveRefreshToken(user.getId(), refreshTokenStr, ttlSeconds);

        return AuthDto.from(
                user,
                accessToken,
                refreshTokenStr,
                ttlSeconds
        );
    }

    /**
     * 리프레시 토큰을 갱신합니다. 기존 토큰을 삭제한 후 새 토큰을 발급합니다.(RTR 전략)
     */
    public AuthDto refreshTokens(String refreshTokenStr) {
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshTokenStr);
        User user = userValidator.findByIdOrThrow(userId);

        refreshTokenRedisManager.validateRefreshToken(userId, refreshTokenStr);

        refreshTokenRedisManager.deleteRefreshToken(user.getId());

        return createTokens(user);
    }

    /**
     * 로그아웃 시 토큰 삭제
     */
    public void deleteRefreshTokens(Long userId) {
        refreshTokenRedisManager.deleteRefreshToken(userId);
    }

    /**
     * [테스트 전용] 이메일을 통한 즉석 로그인
     */
    public AuthDto loginByEmailForTest(String email) {
        String testSub = "test_sub_" + email.split("@")[0];
        User user = userService.getOrCreateUser(testSub, email);

        return createTokens(user);
    }
}