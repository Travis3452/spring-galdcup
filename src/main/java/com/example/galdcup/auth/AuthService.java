package com.example.galdcup.auth;

import com.example.galdcup.auth.dto.AuthDto;
import com.example.galdcup.common.client.GoogleOAuthClient;
import com.example.galdcup.common.security.AES256Encryptor;
import com.example.galdcup.common.security.JwtTokenProvider;
import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuthClient googleClient;
    private final AES256Encryptor encryptor;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    /** 구글 OAuth 콜백 처리 */
    @Transactional
    public AuthDto handleGoogleCallback(String code) {
        var tokens = googleClient.exchangeCodeForToken(code, googleRedirectUri);
        var profile = googleClient.fetchUserProfile(tokens.accessToken());

        String hashOauthId = DigestUtils.sha256Hex(profile.sub());

        User user = userRepository.findByHashOauthId(hashOauthId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .encryptedOauthId(encryptor.encrypt(profile.sub()))
                            .hashOauthId(hashOauthId)
                            .encryptedEmail(encryptor.encrypt(profile.email()))
                            .hashEmail(DigestUtils.sha256Hex(profile.email()))
                            .nickname(profile.name())
                            .role(User.Role.USER)
                            .build();
                    return userRepository.save(newUser);
                });

        return createTokens(user);
    }

    /** 로그인/갱신 시 토큰 발급 */
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

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                Collections.singletonList(user.getRole().name())
        );

        return AuthDto.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getRefreshTokenMaxAgeSeconds(),
                user.getNickname()
        );
    }

    /** RefreshToken으로 새 토큰 발급 */
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

    /** RefreshToken 삭제 */
    @Transactional
    public void deleteRefreshTokens(Long userId) {
        redisTemplate.delete("refreshToken:" + userId);
    }
}