package com.example.galdcup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/me").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth -> oauth.defaultSuccessUrl("/"))
                .sessionManagement(session -> session.sessionFixation().migrateSession());

        return http.build();
    }
}
