package com.example.galdcup.board.voteSession.validator;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.vote.domain.VoteOption;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.domain.VoteSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoteSessionValidatorTests {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-01-01T12:00:00+09:00");
    private final Board board = Board.builder().id(10L).status(Board.Status.OPEN).build();
    private final VoteSession session = VoteSession.builder().id(100L).board(board)
            .startTime(now.minusHours(1)).endTime(now.plusHours(1))
            .options(List.of(VoteOption.create("A", "a.png"), VoteOption.create("B", "b.png")))
            .build();
    private final VoteSessionRepository repository = mock(VoteSessionRepository.class);
    private final VoteSessionValidator validator = new VoteSessionValidator(repository);

    @BeforeEach
    void loadRequestedSession() {
        when(repository.findWithOptionsAndBoardById(100L)).thenReturn(Optional.of(session));
    }

    @Test
    void acceptsValidVoteForRequestedSession() {
        assertSame(session, validator.validateAndGetVotingSession(10L, 100L, 1, now));
    }

    @Test
    void acceptsVoteAtExactStartTime() {
        assertSame(session, validator.validateAndGetVotingSession(10L, 100L, 0, session.getStartTime()));
    }

    @Test
    void rejectsSessionFromAnotherBoard() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateAndGetVotingSession(20L, 100L, 0, now));
    }

    @Test
    void rejectsClosedBoard() {
        board.closeBoard();
        assertThrows(IllegalStateException.class,
                () -> validator.validateAndGetVotingSession(10L, 100L, 0, now));
    }

    @Test
    void rejectsVoteBeforeStart() {
        assertThrows(IllegalStateException.class,
                () -> validator.validateAndGetVotingSession(10L, 100L, 0, session.getStartTime().minusNanos(1)));
    }

    @Test
    void rejectsVoteAtOrAfterEnd() {
        for (OffsetDateTime time : List.of(session.getEndTime(), session.getEndTime().plusSeconds(1))) {
            assertThrows(IllegalStateException.class,
                    () -> validator.validateAndGetVotingSession(10L, 100L, 0, time));
        }
    }

    @Test
    void rejectsFinishedSessionEvenBeforeOriginalDeadline() {
        session.complete();
        assertThrows(IllegalStateException.class,
                () -> validator.validateAndGetVotingSession(10L, 100L, 0, now));
    }

    @Test
    void rejectsNegativeAndOutOfRangeOptions() {
        for (int index : new int[]{-1, 2, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class,
                    () -> validator.validateAndGetVotingSession(10L, 100L, index, now));
        }
    }

    @Test
    void rejectsMissingSession() {
        when(repository.findWithOptionsAndBoardById(100L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateAndGetVotingSession(10L, 100L, 0, now));
    }
}
