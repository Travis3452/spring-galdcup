package com.example.galdcup.auth;

import com.example.galdcup.auth.response.AuthDto;
import com.example.galdcup.auth.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Profile("!prod")
@Tag(name = "Test Auth", description = "개발용 인증 (쿠키 자동 설정)")
public class TestAuthController {

    private final AuthService authService;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.sameSite}")
    private String cookieSameSite;

    /**
     * 이메일 프리패스 로그인
     * Swagger에서 Execute만 누르면 브라우저에 쿠키가 구워집니다.
     */
    @Operation(summary = "이메일 프리패스 로그인", description = "이메일을 입력하면 해당 유저의 권한으로 즉시 쿠키를 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> bypassLogin(@RequestParam String email) {

        AuthDto result = authService.loginByEmailForTest(email);

        ResponseCookie refreshCookie = createCookie("refreshToken", result.refreshToken(), result.refreshTokenMaxAge());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.profile());
    }

    /**
     * AuthController의 쿠키 생성 로직과 동일하게 유지
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