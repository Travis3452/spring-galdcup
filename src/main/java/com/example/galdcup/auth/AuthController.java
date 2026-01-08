package com.example.galdcup.auth;

import com.example.galdcup.auth.dto.AuthDto;
import com.example.galdcup.common.security.CustomUserDetails;
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

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @GetMapping("/callback/google")
    public void googleCallback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {
        AuthDto result = authService.handleGoogleCallback(code);

        Cookie refreshCookie = new Cookie("refreshToken", result.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
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

    @PostMapping("/refresh")
    public ResponseEntity<AuthDto> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthDto result = authService.refreshTokens(refreshToken);

        Cookie refreshCookie = new Cookie("refreshToken", result.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
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
        refreshCookie.setSecure(cookieSecure);   // ✅ 환경별 분리
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);

        return ResponseEntity.noContent().build();
    }
}