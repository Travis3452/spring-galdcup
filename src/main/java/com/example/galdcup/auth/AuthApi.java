package com.example.galdcup.auth;

import com.example.galdcup.auth.response.AuthResponse;
import com.example.galdcup.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth", description = "인증 및 토큰 관리 API")
public interface AuthApi {

    @Operation(summary = "구글 OAuth2 콜백", description = "구글 로그인 성공 후 리다이렉트된 코드로 로그인을 처리합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공 및 쿠키 발급")
    ResponseEntity<AuthResponse> googleCallback(
            @Parameter(description = "구글 인증 코드") @RequestParam("code") String code
    );

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다.")
    ResponseEntity<AuthResponse> refresh(
            @Parameter(hidden = true) @CookieValue(value = "refreshToken", required = false) String refreshToken
    );

    @Operation(summary = "로그아웃", description = "기존에 발급된 리프레시 토큰과 액세스 토큰을 즉시 만료합니다.")
    ResponseEntity<Void> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user
    );
}