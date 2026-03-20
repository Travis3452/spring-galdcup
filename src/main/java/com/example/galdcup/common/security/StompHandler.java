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

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwt = extractToken(accessor);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
                accessor.setUser(authentication);
                log.info("WebSocket 연결 성공 (회원): UserID = {}", jwtTokenProvider.getUserIdFromToken(jwt));
            } else {
                log.info("WebSocket 연결 성공 (비회원/Guest)");
            }
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            log.warn("허용되지 않은 WebSocket SEND 요청 접근 시도 차단");
            throw new AccessDeniedException("클라이언트에서 직접 메시지를 발송할 수 없습니다.");
        }

        return message;
    }

    /**
     * STOMP CONNECT 헤더에서 토큰을 추출합니다.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}