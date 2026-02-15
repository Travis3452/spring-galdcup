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

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuthClient googleClient;
    private final AES256Encryptor encryptor;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-expiration-days}")
    private int refreshExpDays;

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
                            .nickname(generateUniqueNickname())
                            .role(User.Role.MANAGER)
                            .build();
                    return userRepository.save(newUser);
                });

        return createTokens(user);
    }

    /** 기본 닉네임 생성 */
    private String generateUniqueNickname() {
        String nickname;
        do {
            nickname = "user-" + UUID.randomUUID().toString().substring(0, 8);
        } while (userRepository.existsByNickname(nickname));
        return nickname;
    }

    /** 로그인/갱신 시 토큰 발급 */
    @Transactional
    public AuthDto createTokens(User user) {
        String refreshTokenStr = jwtTokenProvider.createRefreshToken(user.getId());

        String redisKey = "refreshToken:" + user.getId();
        long ttlSeconds = refreshExpDays * 24L * 60L * 60L;

        redisTemplate.opsForValue().set(redisKey, refreshTokenStr, Duration.ofSeconds(ttlSeconds));

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                Collections.singletonList(user.getRole().name())
        );

        return AuthDto.of(
                accessToken,
                refreshTokenStr,
                jwtTokenProvider.getRefreshTokenMaxAgeSeconds(),
                user.getNickname()
        );
    }

    /** RefreshToken으로 새 토큰 발급 */
    public AuthDto refreshTokens(String refreshTokenStr) {
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshTokenStr);
        String redisKey = "refreshToken:" + userId;

        String storedToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null || !storedToken.equals(refreshTokenStr)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        redisTemplate.delete(redisKey);

        return createTokens(user);
    }

    /** RefreshToken 삭제 */
    @Transactional
    public void deleteRefreshTokens(Long userId) {
        redisTemplate.delete("refreshToken:" + userId);
    }
}