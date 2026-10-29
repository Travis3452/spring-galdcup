package com.example.galdcup.board.vote.scheduler;

import com.example.galdcup.board.vote.redis.VoteRedisManager;
import com.example.galdcup.board.voteSession.VoteSessionService;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.domain.VoteSessionRepository;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.Mockito.*;

class VoteSyncSchedulerTests {

    private final VoteSessionRepository repository = mock(VoteSessionRepository.class);
    private final VoteSessionService service = mock(VoteSessionService.class);
    private final VoteRedisManager redis = mock(VoteRedisManager.class);
    private final VoteSyncScheduler scheduler = new VoteSyncScheduler(repository, service, redis);

    @Test
    void malformedIdAndFailedSessionDoNotPreventOtherSessionsFromSyncing() {
        when(redis.getDirtySessionIds()).thenReturn(new LinkedHashSet<>(List.of("invalid", "100", "200")));
        doThrow(new IllegalStateException("DB unavailable")).when(service).syncRedisVotesToDb(100L);

        scheduler.syncVotesToDb();

        verify(service).syncRedisVotesToDb(100L);
        verify(service).syncRedisVotesToDb(200L);
        verify(redis, never()).acknowledgeSnapshot(anyLong(), any(), anyBoolean());
    }

    @Test
    void failedCloseDoesNotPreventAnotherSessionFromClosing() {
        when(repository.findByEndTimeBeforeAndIsFinishedFalse(any())).thenReturn(List.of(
                VoteSession.builder().id(100L).build(), VoteSession.builder().id(200L).build()));
        doThrow(new IllegalStateException("DB unavailable")).when(service).finishExpiredVoteSession(100L);

        scheduler.finishVoteSessions();

        verify(service).finishExpiredVoteSession(100L);
        verify(service).finishExpiredVoteSession(200L);
    }
}
