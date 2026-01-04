package com.example.galdcup.controller;

import com.example.galdcup.dto.auth.AuthDto;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${frontend.url}")
    private String frontendUrl;

    // 구글 OAuth 콜백 처리
    @GetMapping("/callback/google")
    public void googleCallback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {
        AuthDto result = authService.handleGoogleCallback(code);

        Cookie refreshCookie = new Cookie("refreshToken", result.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(result.refreshTokenMaxAge());
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);

        String redirectUrl = String.format(
                "%s/auth/callback/google?accessToken=%s&nickname=%s",
                frontendUrl,
                URLEncoder.encode(result.accessToken(), StandardCharsets.UTF_8),
                URLEncoder.encode(result.nickname(), StandardCharsets.UTF_8)
        );

        response.sendRedirect(redirectUrl);
    }

    // RefreshToken으로 AccessToken 갱신
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthDto result = authService.refreshTokens(refreshToken);

        Cookie refreshCookie = new Cookie("refreshToken", result.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(result.refreshTokenMaxAge());
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletResponse response
    ) {
        authService.deleteRefreshTokens(user.getId());

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);

        return ResponseEntity.noContent().build();
    }
}