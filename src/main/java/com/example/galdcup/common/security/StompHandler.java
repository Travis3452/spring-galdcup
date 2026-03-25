package com.example.galdcup.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 웹소켓 연결 및 메시지 전송 시 보안 검증을 수행하는 인터셉터
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    /** 메시지 전송 전 인증 처리 및 요청 권한 검증 */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // 웹소켓 연결 시 인증 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwt = extractToken(accessor);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
                accessor.setUser(authentication);
                log.info("웹소켓 연결 성공 - 인증 회원(ID: {})", jwtTokenProvider.getUserIdFromToken(jwt));
            } else {
                log.info("웹소켓 연결 성공 - 비회원 게스트");
            }
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            log.warn("허용되지 않은 웹소켓 SEND 요청 차단 시도");
            throw new AccessDeniedException("클라이언트에서 직접 메시지를 발송할 수 없는 구역");
        }

        return message;
    }

    /** STOMP CONNECT 헤더의 Authorization 필드에서 토큰 추출 */
    private String extractToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}