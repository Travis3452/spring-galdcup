package com.example.galdcup.service;

import com.example.galdcup.client.GoogleOAuthClient;
import com.example.galdcup.dto.auth.AuthDto;
import com.example.galdcup.entity.RefreshToken;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.RefreshTokenRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final GoogleOAuthClient googleClient;

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
    public AuthDto createTokens(User user) {
        refreshTokenRepository.deleteByUser(user);

        String refresh = jwtService.createRefreshToken(user);
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .token(refresh)
                .expiryDate(LocalDateTime.now().plusDays(jwtService.getRefreshExpDays()))
                .build();
        refreshTokenRepository.save(entity);

        String access = jwtService.createAccessToken(user);

        return AuthDto.of(access, refresh, jwtService.getRefreshTokenMaxAgeSeconds());
    }

    public AuthDto refreshTokens(String refreshToken) {
        var claims = jwtService.parseRefreshToken(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return createTokens(user);
    }
}