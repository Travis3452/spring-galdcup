package com.example.galdcup.board;

import com.example.galdcup.board.ban.*;
import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BoardBanNoticeTests {
    @Test void timedBanIncludesReasonAndPeriod() {
        var repository = mock(BoardBanRepository.class);
        var now = OffsetDateTime.now();
        var ban = BoardBan.create(Board.builder().id(1L).build(), 2L, "writer", "도배", now.plusDays(2), 3L, now);
        when(repository.findActive(eq(1L), eq(2L), any(), any())).thenReturn(List.of(ban));
        var exception = assertThrows(BoardBannedException.class, () -> new BoardWriteGuard(repository).requireAllowed(1L, 2L));
        var response = new GlobalExceptionHandler().handleBoardBanned(exception);
        assertEquals(403, response.getStatusCode().value());
        assertEquals("BOARD_BANNED", response.getBody().get("error"));
        var details = (Map<?, ?>) response.getBody().get("ban");
        assertEquals("도배", details.get("reason"));
        assertEquals(now, details.get("startsAt"));
        assertEquals(now.plusDays(2), details.get("expiresAt"));
        assertEquals(false, details.get("permanent"));
    }

    @Test void permanentBanIsExplicit() {
        var ban = BoardBan.create(Board.builder().id(1L).build(), 2L, "writer", "반복 위반", null, 3L, OffsetDateTime.now());
        var response = new GlobalExceptionHandler().handleBoardBanned(new BoardBannedException(1L, ban));
        var details = (Map<?, ?>) response.getBody().get("ban");
        assertEquals(true, details.get("permanent"));
        assertNull(details.get("expiresAt"));
    }

    @Test void noActiveBanAllowsWriting() {
        var repository = mock(BoardBanRepository.class);
        when(repository.findActive(any(), any(), any(), any())).thenReturn(List.of());
        assertDoesNotThrow(() -> new BoardWriteGuard(repository).requireAllowed(1L, 2L));
    }

    @Test void ordinaryAccessDenialRemainsDistinct() {
        var response = new GlobalExceptionHandler().handleAccessDenied(new AccessDeniedException("denied"));
        assertEquals("ACCESS_DENIED", response.getBody().get("error"));
        assertFalse(response.getBody().containsKey("ban"));
    }
}
