package com.example.galdcup.auth;

import com.example.galdcup.auth.response.AuthDto;
import com.example.galdcup.auth.response.AuthProfileResponse;
import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.sameSite}")
    private String cookieSameSite;

    @Value("${jwt.access-expiration:3600}")
    private long accessTokenMaxAge;

    /**
     * 구글 OAuth 콜백
     */
    @PostMapping("/callback/google")
    public ResponseEntity<AuthProfileResponse> googleCallback(@RequestParam("code") String code) {
        AuthDto result = authService.handleGoogleCallback(code);

        ResponseCookie refreshCookie = createCookie("refreshToken", result.refreshToken(), result.refreshTokenMaxAge());
        ResponseCookie accessCookie = createCookie("accessToken", result.accessToken(), accessTokenMaxAge);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(result.profile());
    }

    /**
     * 토큰 재발급 (Refresh)
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PostMapping("/refresh")
    public ResponseEntity<AuthProfileResponse> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthDto result = authService.refreshTokens(refreshToken);

        ResponseCookie refreshCookie = createCookie("refreshToken", result.refreshToken(), result.refreshTokenMaxAge());
        ResponseCookie accessCookie = createCookie("accessToken", result.accessToken(), accessTokenMaxAge);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(result.profile());
    }

    /**
     * 로그아웃
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails user) {
        if (user != null) {
            authService.deleteRefreshTokens(user.getId());
        }

        ResponseCookie refreshCookie = createCookie("refreshToken", "", 0);
        ResponseCookie accessCookie = createCookie("accessToken", "", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .build();
    }

    /**
     * 쿠키 생성 헬퍼 메서드
     */
    private ResponseCookie createCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}