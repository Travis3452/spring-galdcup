package com.example.galdcup.board.vote.redis;

import com.example.galdcup.board.vote.domain.Vote;
import com.example.galdcup.board.vote.domain.VoteOption;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** 실제 Redis에 쓰므로 명시적으로 지정한 전용 로컬 테스트 포트에서만 실행. */
@Tag("redis-integration")
@EnabledIfEnvironmentVariable(named = "GALDCUP_VOTE_TEST_REDIS_PORT", matches = "[0-9]+")
class VoteRedisIntegrationTests {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private static VoteRedisManager manager;
    private static final String DIRTY = "galdcup:vote:dirty-sessions";
    private final Set<Long> users = ConcurrentHashMap.newKeySet();
    private Long sessionId;
    private VoteSession session;

    @BeforeAll
    static void connectToExplicitTestRedis() {
        int port = Integer.parseInt(System.getenv("GALDCUP_VOTE_TEST_REDIS_PORT"));
        assertTrue(port > 1024 && port <= 65535 && port != 6379,
                "기본 Redis 포트가 아닌 전용 로컬 테스트 포트를 지정해야 합니다.");
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("127.0.0.1", port),
                LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(3)).build());
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        manager = new VoteRedisManager(redis);
        manager.init();
    }

    @AfterAll
    static void closeConnections() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void createSession() {
        sessionId = Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1000000000000L) + 1000000000000L;
        VoteOption first = VoteOption.create("A", "a.png");
        VoteOption second = VoteOption.create("B", "b.png");
        first.updateCount(10);
        second.updateCount(20);
        session = VoteSession.builder().id(sessionId)
                .startTime(OffsetDateTime.now().minusMinutes(1))
                .endTime(OffsetDateTime.now().plusMinutes(5))
                .options(List.of(first, second)).build();
    }

    @AfterEach
    void deleteOnlyThisTestsKeys() {
        if (sessionId == null || redis == null) {
            return;
        }
        List<String> keys = new ArrayList<>(List.of(key("counts"), key("revision"), key("closed")));
        users.forEach(user -> keys.add(key("user:" + user)));
        redis.delete(keys);
        redis.opsForSet().remove(DIRTY, sessionId.toString());
    }

    @Test
    void concurrentFirstVotesCannotOverwriteEachOthersCounts() throws Exception {
        assertEquals(30, concurrentVotes(30, false));

        VoteCountSnapshot snapshot = manager.getSnapshot(sessionId);
        assertEquals("40", snapshot.counts().get("0"));
        assertEquals("20", snapshot.counts().get("1"));
        assertEquals("30", snapshot.revision());
    }

    @Test
    void concurrentDuplicateVotesAreRecordedOnlyOnce() throws Exception {
        assertEquals(1, concurrentVotes(20, true));
        assertEquals(31L, manager.getTotalVoteCount(sessionId));
    }

    @Test
    void newVotePreventsAcknowledgingAnOlderSnapshot() {
        cast(1L, 0);
        VoteCountSnapshot old = manager.getSnapshot(sessionId);
        cast(2L, 0);

        assertFalse(manager.acknowledgeSnapshot(sessionId, old, false));
        assertTrue(manager.getDirtySessionIds().contains(sessionId.toString()));
        assertEquals(32L, manager.getTotalVoteCount(sessionId));

        assertTrue(manager.acknowledgeSnapshot(sessionId, manager.getSnapshot(sessionId), false));
        assertFalse(manager.getDirtySessionIds().contains(sessionId.toString()));
    }

    @Test
    void closeFenceRejectsRequestValidatedBeforeClosing() {
        cast(1L, 0);
        manager.closeAndGetSnapshot(session);

        assertThrows(IllegalStateException.class, () -> cast(2L, 0));
        assertEquals(31L, manager.getTotalVoteCount(sessionId));
    }

    @Test
    void concurrentCloseIncludesEveryAcceptedVoteInFinalSnapshot() throws Exception {
        cast(100L, 0);
        int accepted = concurrentVotes(30, false, true);

        VoteCountSnapshot finalCounts = manager.getSnapshot(sessionId);
        assertNotNull(finalCounts.closedAtEpochMilli());
        assertEquals(11L + accepted, Long.parseLong(finalCounts.counts().get("0")));
        assertEquals("20", finalCounts.counts().get("1"));
    }

    @Test
    void openSnapshotCannotRemovePendingCloseWithoutAnyNewVotes() {
        cast(1L, 0);
        VoteCountSnapshot open = manager.getSnapshot(sessionId);
        manager.closeAndGetSnapshot(session);

        assertFalse(manager.acknowledgeSnapshot(sessionId, open, false));
        assertTrue(manager.getDirtySessionIds().contains(sessionId.toString()));
    }

    @Test
    void uncommittedCloseRetainsCountsAndOriginalCloseTimeForRetry() {
        cast(1L, 0);
        VoteCountSnapshot first = manager.closeAndGetSnapshot(session);
        VoteCountSnapshot retried = manager.closeAndGetSnapshot(session);

        assertEquals(first, retried);
        assertEquals(31L, manager.getTotalVoteCount(sessionId));
        assertTrue(manager.getDirtySessionIds().contains(sessionId.toString()));
    }

    @Test
    void committedCloseDeletesCountsButKeepsFenceAgainstStaleRequests() {
        cast(1L, 0);
        VoteCountSnapshot closed = manager.closeAndGetSnapshot(session);

        assertTrue(manager.acknowledgeSnapshot(sessionId, closed, true));

        assertTrue(manager.getSnapshot(sessionId).counts().isEmpty());
        assertNotNull(manager.getSnapshot(sessionId).closedAtEpochMilli());
        assertFalse(manager.getDirtySessionIds().contains(sessionId.toString()));
        assertThrows(IllegalStateException.class, () -> cast(2L, 0));
    }

    @Test
    void invalidOptionIsRejectedBeforeCreatingCountsOrDirtyEntry() {
        assertThrows(IllegalArgumentException.class, () -> cast(1L, 2));
        assertTrue(manager.getSnapshot(sessionId).counts().isEmpty());
        assertFalse(manager.getDirtySessionIds().contains(sessionId.toString()));
    }

    @Test
    void expiredSessionIsRejectedAtRedisEvenWithoutJavaValidation() {
        session.setEndTime(OffsetDateTime.now().minusSeconds(1));

        assertThrows(IllegalStateException.class, () -> cast(1L, 0));
        assertTrue(manager.getSnapshot(sessionId).counts().isEmpty());
    }

    @Test
    void futureSessionIsRejectedAtRedisEvenWithoutJavaValidation() {
        session.setStartTime(OffsetDateTime.now().plusMinutes(1));

        assertThrows(IllegalStateException.class, () -> cast(1L, 0));
        assertTrue(manager.getSnapshot(sessionId).counts().isEmpty());
    }

    @Test
    void unsavedCountsLoseTheirOldExpiryInsteadOfExpiringDuringDbOutage() {
        cast(1L, 0);
        redis.expire(key("counts"), Duration.ofSeconds(30));
        cast(2L, 0);

        assertEquals(-1L, redis.getExpire(key("counts")));
        assertEquals(32L, manager.getTotalVoteCount(sessionId));
    }

    private void cast(Long userId, int option) {
        users.add(userId);
        manager.castVote(Vote.of(sessionId, userId, option, 600L), session);
    }

    private int concurrentVotes(int count, boolean sameUser) throws Exception {
        return concurrentVotes(count, sameUser, false);
    }

    private int concurrentVotes(int count, boolean sameUser, boolean close) throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                long userId = sameUser ? 1L : i + 1L;
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        cast(userId, 0);
                        return true;
                    } catch (IllegalStateException rejected) {
                        return false;
                    }
                }));
            }
            if (close) {
                results.add(executor.submit(() -> {
                    start.await();
                    manager.closeAndGetSnapshot(session);
                    return false;
                }));
            }
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            return accepted;
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private String key(String suffix) {
        return "galdcup:vote:session:" + sessionId + ":" + suffix;
    }
}
