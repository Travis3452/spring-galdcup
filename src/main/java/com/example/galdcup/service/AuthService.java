package com.example.galdcup.service;

import com.example.galdcup.client.GoogleOAuthClient;
import com.example.galdcup.dto.auth.AuthDto;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final GoogleOAuthClient googleClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public AuthDto handleGoogleCallback(String code, String redirectUri) {
        var tokens = googleClient.exchangeCodeForToken(code, redirectUri);
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

    /**
     * 로그인/갱신 시 토큰 발급
     */
    @Transactional
    public AuthDto createTokens(User user) {
        redisTemplate.delete("refresh:" + user.getId());

        String refreshToken = jwtService.createRefreshToken(user.getId());
        redisTemplate.opsForValue().set(
                "refreshToken:" + user.getId(),
                refreshToken,
                jwtService.getRefreshTokenMaxAgeSeconds(),
                TimeUnit.SECONDS
        );

        String accessToken = jwtService.createAccessToken(user.getId());

        return AuthDto.of(accessToken, refreshToken, jwtService.getRefreshTokenMaxAgeSeconds());
    }

    @Transactional
    public AuthDto refreshTokens(String refreshToken) {
        var claims = jwtService.parseRefreshToken(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());

        String storedToken = redisTemplate.opsForValue().get("refresh:" + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return createTokens(user);
    }
}