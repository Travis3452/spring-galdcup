package com.example.galdcup.common.config;

import com.example.galdcup.common.security.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 웹소켓 메시징 설정
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    private final StompHandler stompHandler;

    /** 메시지 구독 및 발행을 위한 내장 브로커 설정 */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 메시지를 구독할 prefix 정의
        config.enableSimpleBroker("/topic");
    }

    /** 클라이언트가 웹소켓 연결을 시작할 엔드포인트 등록 */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-galdcup")
                .setAllowedOriginPatterns(allowedOrigins);
    }

    /** 클라이언트로부터 들어오는 메시지에 대한 보안 및 인증 인터셉터 등록 */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 연결 시 JWT 인증 등을 처리하는 StompHandler
        registration.interceptors(stompHandler);
    }
}