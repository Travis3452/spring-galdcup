package com.example.galdcup.board.vote;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.vote.domain.Vote;
import com.example.galdcup.board.vote.domain.VoteOption;
import com.example.galdcup.board.vote.redis.VoteRedisManager;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.domain.VoteSessionRepository;
import com.example.galdcup.board.voteSession.validator.VoteSessionValidator;
import com.example.galdcup.user.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoteServiceTests {

    private final VoteSessionRepository repository = mock(VoteSessionRepository.class);
    private final UserValidator users = mock(UserValidator.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final VoteRedisManager redis = mock(VoteRedisManager.class);
    private final VoteService service = new VoteService(
            new VoteSessionValidator(repository), users, messaging, redis);
    private final VoteSession session = VoteSession.builder().id(100L)
            .board(Board.builder().id(10L).status(Board.Status.OPEN).build())
            .startTime(OffsetDateTime.now().minusHours(1)).endTime(OffsetDateTime.now().plusHours(1))
            .options(List.of(VoteOption.create("A", "a.png"), VoteOption.create("B", "b.png")))
            .build();

    @BeforeEach
    void loadSession() {
        when(repository.findWithOptionsAndBoardById(100L)).thenReturn(Optional.of(session));
    }

    @Test
    void validVoteUsesValidatedSessionAndUser() {
        var result = service.createVote(10L, 100L, 7L, 1);

        ArgumentCaptor<Vote> vote = ArgumentCaptor.forClass(Vote.class);
        verify(redis).castVote(vote.capture(), same(session));
        assertEquals(100L, vote.getValue().getVoteSessionId());
        assertEquals(7L, vote.getValue().getUserId());
        assertEquals(1, result.selectedOptionIndex());
    }

    @Test
    void foreignSessionIsRejectedBeforeRedisOrUserAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.createVote(20L, 100L, 7L, 0));
        verifyNoInteractions(redis, users, messaging);
    }

    @Test
    void redisRejectionOfConcurrentCloseDoesNotBroadcastSuccess() {
        doThrow(new IllegalStateException("closed"))
                .when(redis).castVote(any(Vote.class), same(session));

        assertThrows(IllegalStateException.class, () -> service.createVote(10L, 100L, 7L, 0));
        verifyNoInteractions(messaging);
    }

    @Test
    void notificationFailureDoesNotReportRecordedVoteAsFailed() {
        doThrow(new IllegalStateException("broker unavailable"))
                .when(messaging).convertAndSend(eq("/topic/votes/100"), any(Object.class));

        var result = assertDoesNotThrow(() -> service.createVote(10L, 100L, 7L, 0));

        assertEquals("100:7", result.id());
        verify(redis).castVote(any(Vote.class), same(session));
    }
}
