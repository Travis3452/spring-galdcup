package com.example.galdcup.controller;

import com.example.galdcup.dto.auth.AuthDto;
import com.example.galdcup.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 구글 OAuth 콜백 처리
     */
    @GetMapping("/callback/google")
    public ResponseEntity<AuthDto> googleCallback(
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri,
            HttpServletResponse response
    ) {
        AuthDto result = authService.handleGoogleCallback(code, redirectUri);

        // RefreshToken 쿠키 설정
        Cookie refreshCookie = new Cookie("refreshToken", result.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/api/auth");
        refreshCookie.setMaxAge(result.refreshTokenMaxAge());
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(result);
    }

    /**
     * RefreshToken으로 AccessToken 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthDto result = authService.refreshTokens(refreshToken);

        Cookie refreshCookie = new Cookie("refreshToken", result.refreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/api/auth");
        refreshCookie.setMaxAge(result.refreshTokenMaxAge());
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(result);
    }
}