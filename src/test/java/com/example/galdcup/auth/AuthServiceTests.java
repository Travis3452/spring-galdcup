package com.example.galdcup.auth;

import com.example.galdcup.auth.redis.RefreshTokenRedisManager;
import com.example.galdcup.common.client.GoogleOAuthClient;
import com.example.galdcup.common.security.JwtTokenProvider;
import com.example.galdcup.user.UserService;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTests {

    private final UserService userService = mock(UserService.class);
    private final JwtTokenProvider provider = new JwtTokenProvider(
            "test-only-signing-key-0123456789abcdef0123456789abcdef0123456789", 3600000L, 7);
    private final GoogleOAuthClient googleClient = mock(GoogleOAuthClient.class);
    private final RefreshTokenRedisManager tokens = mock(RefreshTokenRedisManager.class);
    private final UserValidator userValidator = mock(UserValidator.class);
    private final AuthService service = new AuthService(
            userService, provider, googleClient, tokens, userValidator);

    @Test
    void accessTokenIsRejectedBeforeAnySessionOrUserLookup() {
        String token = provider.createAccessToken(1L, List.of("USER"));

        assertThrows(BadCredentialsException.class, () -> service.refreshTokens(token));

        verifyNoInteractions(tokens, userValidator, userService, googleClient);
    }

    @Test
    void revokedRefreshTokenCannotIssueNewTokens() {
        String token = provider.createRefreshToken(1L);
        doThrow(new BadCredentialsException("revoked"))
                .when(tokens).validateRefreshToken(1L, token);

        assertThrows(BadCredentialsException.class, () -> service.refreshTokens(token));

        verify(tokens).validateRefreshToken(1L, token);
        verifyNoMoreInteractions(tokens);
        verifyNoInteractions(userValidator, userService, googleClient);
    }

    @Test
    void registeredRefreshTokenIssuesCorrectlyTypedTokens() {
        String token = provider.createRefreshToken(1L);
        User user = User.builder().id(1L).role(User.Role.USER).build();
        when(userValidator.findByIdOrThrow(1L)).thenReturn(user);

        var result = service.refreshTokens(token);

        assertEquals("1", provider.getAccessAuthentication(result.profile().accessToken()).getName());
        assertEquals(1L, provider.getUserIdFromRefreshToken(result.refreshToken()));
        var order = inOrder(tokens, userValidator);
        order.verify(tokens).validateRefreshToken(1L, token);
        order.verify(userValidator).findByIdOrThrow(1L);
        order.verify(tokens).deleteRefreshToken(1L);
        order.verify(tokens).saveRefreshToken(1L, result.refreshToken(), 604800L);
    }
}
