package com.example.galdcup.board.voteSession;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.board.vote.domain.VoteOption;
import com.example.galdcup.board.vote.redis.VoteCountSnapshot;
import com.example.galdcup.board.vote.redis.VoteRedisManager;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.domain.VoteSessionRepository;
import com.example.galdcup.board.voteSession.redis.VoteSessionRedisManager;
import com.example.galdcup.board.voteSession.validator.VoteSessionValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoteSessionServiceTests {

    private final VoteSessionRepository repository = mock(VoteSessionRepository.class);
    private final BoardValidator boards = mock(BoardValidator.class);
    private final VoteSessionRedisManager cache = mock(VoteSessionRedisManager.class);
    private final VoteRedisManager redis = mock(VoteRedisManager.class);
    private final VoteSessionService service = new VoteSessionService(repository, boards,
            mock(VoteSessionValidator.class), cache, redis);
    private final VoteSession session = VoteSession.builder().id(100L)
            .board(Board.builder().id(10L).status(Board.Status.OPEN).build())
            .startTime(OffsetDateTime.now().minusHours(1)).endTime(OffsetDateTime.now().plusHours(1))
            .options(List.of(VoteOption.create("A", "a.png"), VoteOption.create("B", "b.png")))
            .build();
    private final VoteCountSnapshot open = new VoteCountSnapshot(Map.of("0", "4", "1", "7"), "2", null);
    private final VoteCountSnapshot closed = new VoteCountSnapshot(
            open.counts(), "2", System.currentTimeMillis());

    @BeforeEach
    void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        when(repository.findForUpdateById(100L)).thenReturn(Optional.of(session));
    }

    @AfterEach
    void clearTransaction() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void reloadsSessionAndPersistsCountsBeforeAcknowledgingAfterCommit() {
        when(redis.getSnapshot(100L)).thenReturn(open);

        service.syncRedisVotesToDb(100L);

        assertEquals(4L, session.getOptions().get(0).getCount());
        assertEquals(7L, session.getOptions().get(1).getCount());
        assertFalse(session.isFinished());
        verify(repository).findForUpdateById(100L);
        verify(redis, never()).acknowledgeSnapshot(anyLong(), any(), anyBoolean());

        commit();

        verify(redis).acknowledgeSnapshot(100L, open, false);
        verifyNoInteractions(cache);
    }

    @Test
    void manualCloseDoesNotDeleteCountsOrDirtyStateBeforeCommit() {
        when(redis.closeAndGetSnapshot(session)).thenReturn(closed);

        service.finishVoteSession(10L, 100L, 7L);

        assertTrue(session.isFinished());
        assertEquals(4L, session.getOptions().get(0).getCount());
        verify(redis).closeAndGetSnapshot(session);
        verify(redis, never()).acknowledgeSnapshot(anyLong(), any(), anyBoolean());
        verifyNoInteractions(cache);

        commit();

        var order = inOrder(cache, redis);
        order.verify(cache).deleteLatestVoteSession(10L);
        order.verify(cache).deletePastVoteSessions(10L);
        order.verify(redis).acknowledgeSnapshot(100L, closed, true);
    }

    @Test
    void rollbackLeavesRedisCountsAndCloseRequestPending() {
        when(redis.closeAndGetSnapshot(session)).thenReturn(closed);
        service.finishVoteSession(10L, 100L, 7L);

        TransactionSynchronizationManager.getSynchronizations().forEach(
                callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(redis).closeAndGetSnapshot(session);
        verify(redis, never()).acknowledgeSnapshot(anyLong(), any(), anyBoolean());
        verifyNoInteractions(cache);
    }

    @Test
    void dirtySessionWithPendingCloseIsCompletedOnRetry() {
        when(redis.getSnapshot(100L)).thenReturn(closed);
        when(redis.closeAndGetSnapshot(session)).thenReturn(closed);

        service.syncRedisVotesToDb(100L);
        commit();

        assertTrue(session.isFinished());
        assertEquals(closed.closedAtEpochMilli().longValue(), session.getEndTime().toInstant().toEpochMilli());
        verify(redis).acknowledgeSnapshot(100L, closed, true);
    }

    @Test
    void alreadyFinishedSessionRetriesFailedPostCommitCleanup() {
        session.complete();
        when(redis.closeAndGetSnapshot(session)).thenReturn(closed);

        service.syncRedisVotesToDb(100L);
        commit();

        verify(redis).acknowledgeSnapshot(100L, closed, true);
        verify(cache).deleteLatestVoteSession(10L);
    }

    @Test
    void cleanupFailureDoesNotTurnSuccessfulCommitIntoRequestFailure() {
        when(redis.closeAndGetSnapshot(session)).thenReturn(closed);
        doThrow(new IllegalStateException("Redis unavailable")).when(cache).deleteLatestVoteSession(10L);
        service.finishVoteSession(10L, 100L, 7L);

        assertDoesNotThrow(this::commit);

        verify(redis, never()).acknowledgeSnapshot(anyLong(), any(), anyBoolean());
    }

    @Test
    void futureSessionIsNotAutomaticallyClosed() {
        service.finishExpiredVoteSession(100L);

        assertFalse(session.isFinished());
        verifyNoInteractions(redis, cache);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void expiredSessionIsClosedInItsOwnServiceTransaction() {
        OffsetDateTime end = OffsetDateTime.now().minusMinutes(1);
        session.setEndTime(end);

        VoteCountSnapshot finalCounts = new VoteCountSnapshot(
                open.counts(),
                "2",
                end.toInstant().toEpochMilli()
        );

        when(redis.closeAndGetSnapshot(session)).thenReturn(finalCounts);

        service.finishExpiredVoteSession(100L);
        commit();

        assertTrue(session.isFinished());

        assertEquals(
                end.toInstant().toEpochMilli(),
                session.getEndTime().toInstant().toEpochMilli()
        );

        verify(redis).acknowledgeSnapshot(100L, finalCounts, true);
    }

    @Test
    void foreignBoardCannotCloseSessionOrWriteRedis() {
        assertThrows(AccessDeniedException.class, () -> service.finishVoteSession(20L, 100L, 7L));
        verifyNoInteractions(redis, cache);
    }

    @Test
    void invalidCountIsNotAcknowledgedAsSuccessfullySaved() {
        when(redis.getSnapshot(100L)).thenReturn(new VoteCountSnapshot(Map.of("99", "1"), "2", null));

        assertThrows(IllegalStateException.class, () -> service.syncRedisVotesToDb(100L));

        verify(redis, never()).acknowledgeSnapshot(anyLong(), any(), anyBoolean());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    private void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }
}
