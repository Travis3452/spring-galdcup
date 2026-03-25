package com.example.galdcup.common.config;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XSS 공격 방지 및 안전한 HTML 렌더링을 위한 새니타이저 설정
 */
@Configuration
public class HtmlSanitizerConfig {

    /**
     * 허용할 HTML 태그
     */
    @Bean
    public PolicyFactory htmlSanitizer() {
        return Sanitizers.FORMATTING
                .and(Sanitizers.LINKS)
                .and(Sanitizers.BLOCKS)
                .and(Sanitizers.IMAGES)
                .and(Sanitizers.STYLES);
    }
}