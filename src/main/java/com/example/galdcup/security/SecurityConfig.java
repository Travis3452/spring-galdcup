package com.example.galdcup.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // AuthController → 공개
                        .requestMatchers("/api/auth/**").permitAll()

                        // BoardController
                        .requestMatchers(HttpMethod.GET, "/api/boards/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/boards/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/boards/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/boards/**").hasRole("ADMIN")

                        // CommentController
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()

                        // PostController
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/posts/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/posts/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/**").authenticated()

                        // PostReactionController
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/reactions").authenticated()

                        // ReplyController
                        .requestMatchers(HttpMethod.GET, "/api/replies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/replies/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/replies/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/replies/**").authenticated()

                        // UserController
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()

                        // VoteController
                        .requestMatchers(HttpMethod.POST, "/api/votes/**").authenticated()

                        // VoteSessionController
                        .requestMatchers(HttpMethod.POST, "/api/boards/*/vote-session").hasRole("ADMIN")

                        // 그 외 요청은 허용
                        .anyRequest().permitAll()
                )

                // OAuth2 로그인만 허용
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                )

                // 세션 관리 (JWT 기반이면 stateless로)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}