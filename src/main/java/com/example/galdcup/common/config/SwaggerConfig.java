package com.example.galdcup.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger(OpenAPI v3) 전역 설정
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI galdcupOpenAPI() {
        // 1. 보안 스키마 정의
        SecurityScheme cookieAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("accessToken");

        // 2. 보안 요구사항 정의
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("CookieAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("Galdcup API Specification")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes("CookieAuth", cookieAuth))
                .addSecurityItem(securityRequirement);
    }
}