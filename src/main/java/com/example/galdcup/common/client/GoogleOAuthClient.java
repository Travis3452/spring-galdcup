package com.example.galdcup.common.client;

import com.example.galdcup.auth.dto.GoogleTokenDto;
import com.example.galdcup.auth.dto.GoogleUserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class GoogleOAuthClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

    /**
     * Authorization Code → Access Token 교환
     */
    public GoogleTokenDto exchangeCodeForToken(String code, String redirectUri) {
        Map<String, String> request = Map.of(
                "code", code,
                "client_id", clientId,
                "client_secret", clientSecret,
                "redirect_uri", redirectUri,
                "grant_type", "authorization_code"
        );

        return restTemplate.postForObject(
                tokenUri,
                request,
                GoogleTokenDto.class
        );
    }

    /**
     * AccessToken으로 사용자 프로필 조회
     */
    public GoogleUserProfile fetchUserProfile(String accessToken) {
        return restTemplate.getForObject(
                userInfoUri + "?access_token=" + accessToken,
                GoogleUserProfile.class
        );
    }
}