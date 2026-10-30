package com.example.galdcup.common.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.web.context.support.WebApplicationContextUtils;

@Configuration(proxyBeanMethods = false)
public class ActuatorSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new AndRequestMatcher(
                                this::isManagementContext,
                                EndpointRequest.to("health", "prometheus").withHttpMethod(HttpMethod.GET)))
                        .permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.FORBIDDEN)))
                .build();
    }

    private boolean isManagementContext(HttpServletRequest request) {
        // 요청 헤더나 URL의 포트 대신 실제 서블릿 컨텍스트로 관리 서버를 식별한다.
        var context = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
        return WebServerApplicationContext.hasServerNamespace(context, WebServerNamespace.MANAGEMENT.getValue());
    }
}
