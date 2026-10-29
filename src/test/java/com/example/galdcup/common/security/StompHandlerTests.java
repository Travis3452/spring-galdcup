package com.example.galdcup.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class StompHandlerTests {

    private final JwtTokenProvider provider = new JwtTokenProvider(
            "test-only-signing-key-0123456789abcdef0123456789abcdef0123456789", 3600000L, 7);
    private final StompHandler handler = new StompHandler(provider);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void accessTokenAuthenticatesConnection() {
        Message<byte[]> message = connect(provider.createAccessToken(1L, List.of("USER")));

        handler.preSend(message, channel);

        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        assertNotNull(accessor);
        assertNotNull(accessor.getUser());
        assertEquals("1", accessor.getUser().getName());
    }

    @Test
    void refreshTokenCannotAuthenticateConnection() {
        Message<byte[]> message = connect(provider.createRefreshToken(1L));

        assertThrows(AccessDeniedException.class, () -> handler.preSend(message, channel));
    }

    @Test
    void malformedTokenIsRejected() {
        Message<byte[]> message = connect("not.a.jwt");

        assertThrows(AccessDeniedException.class, () -> handler.preSend(message, channel));
    }

    @Test
    void connectionWithoutTokenRemainsGuest() {
        Message<byte[]> message = connect(null);

        assertSame(message, handler.preSend(message, channel));

        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        assertNotNull(accessor);
        assertNull(accessor.getUser());
    }

    private Message<byte[]> connect(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (token != null) {
            accessor.setNativeHeader("Authorization", "Bearer " + token);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
