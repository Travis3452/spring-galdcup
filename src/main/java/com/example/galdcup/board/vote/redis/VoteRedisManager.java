package com.example.galdcup.board.vote.redis;

import com.example.galdcup.board.vote.domain.Vote;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 투표 초기화, 기록, 종료 및 동기화 확인을 원자적으로 처리하는 Redis 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class VoteRedisManager {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> voteScript;
    @SuppressWarnings("rawtypes")
    private DefaultRedisScript<List> snapshotScript;
    private DefaultRedisScript<Long> acknowledgeScript;

    private static final String USER_VOTE_KEY = "galdcup:vote:session:%d:user:%d";
    private static final String VOTE_COUNT_KEY = "galdcup:vote:session:%d:counts";
    private static final String REVISION_KEY = "galdcup:vote:session:%d:revision";
    private static final String CLOSED_KEY = "galdcup:vote:session:%d:closed";
    private static final String DIRTY_SESSIONS_KEY = "galdcup:vote:dirty-sessions";

    @PostConstruct
    public void init() {
        voteScript = script("vote.lua", Long.class);
        snapshotScript = script("vote-snapshot.lua", List.class);
        acknowledgeScript = script("vote-acknowledge.lua", Long.class);
    }

    /**
     * 투표 실행 및 전체 카운트 증가
     */
    public void castVote(Vote vote, VoteSession session) {
        if (!session.getId().equals(vote.getVoteSessionId())) {
            throw new IllegalArgumentException("투표 세션이 일치하지 않습니다.");
        }
        List<String> arguments = new ArrayList<>();
        arguments.add(session.getId().toString());
        arguments.add(String.valueOf(vote.getSelectedOptionIndex()));
        arguments.add(String.valueOf(session.getStartTime().toInstant().toEpochMilli()));
        arguments.add(String.valueOf(session.getEndTime().toInstant().toEpochMilli()));
        arguments.add(String.valueOf(session.getOptions().size()));
        session.getOptions().forEach(option -> arguments.add(String.valueOf(option.getCount())));

        Long result = redisTemplate.execute(voteScript, List.of(
                String.format(USER_VOTE_KEY, session.getId(), vote.getUserId()),
                String.format(VOTE_COUNT_KEY, session.getId()),
                DIRTY_SESSIONS_KEY,
                String.format(CLOSED_KEY, session.getId()),
                String.format(REVISION_KEY, session.getId())), arguments.toArray());

        if (result == null) {
            throw new IllegalStateException("투표 처리 결과를 확인할 수 없습니다.");
        }
        if (result == 0) {
            throw new IllegalStateException("이미 해당 투표에 참여하였습니다.");
        }
        if (result == -2) {
            throw new IllegalArgumentException("유효하지 않은 투표 선택지입니다.");
        }
        if (result != 1) {
            throw new IllegalStateException("현재 투표할 수 없는 세션입니다.");
        }
    }

    public Set<String> getDirtySessionIds() {
        Set<String> ids = redisTemplate.opsForSet().members(DIRTY_SESSIONS_KEY);
        return ids == null ? Set.of() : ids;
    }

    public VoteCountSnapshot getSnapshot(Long sessionId) {
        return readSnapshot(sessionId, "read", 0L);
    }

    /** 종료 요청도 dirty set에 남겨 DB 롤백이나 프로세스 종료 후 재시도 가능하게 함 */
    public VoteCountSnapshot closeAndGetSnapshot(VoteSession session) {
        return readSnapshot(session.getId(), "close", session.getEndTime().toInstant().toEpochMilli());
    }

    private VoteCountSnapshot readSnapshot(Long sessionId, String mode, long endTimeMillis) {
        List<?> result = redisTemplate.execute(snapshotScript, sessionKeys(sessionId),
                mode, sessionId.toString(), String.valueOf(endTimeMillis));
        if (result == null || result.size() < 2 || result.size() % 2 != 0) {
            throw new IllegalStateException("투표 집계를 읽을 수 없습니다.");
        }
        Map<String, String> counts = new HashMap<>();
        for (int i = 2; i < result.size(); i += 2) {
            counts.put(result.get(i).toString(), result.get(i + 1).toString());
        }
        String closedAt = result.get(1).toString();
        return new VoteCountSnapshot(counts, result.get(0).toString(),
                closedAt.isEmpty() ? null : Long.valueOf(closedAt));
    }

    /** DB 커밋 후 호출. 새 투표 또는 종료 요청이 있으면 dirty 상태를 유지 */
    public boolean acknowledgeSnapshot(Long sessionId, VoteCountSnapshot snapshot, boolean finished) {
        Long result = redisTemplate.execute(acknowledgeScript, sessionKeys(sessionId),
                sessionId.toString(), snapshot.revision(), finished ? "1" : "0");
        if (result == null) {
            throw new IllegalStateException("투표 집계의 동기화 완료를 확인할 수 없습니다.");
        }
        return result == 1;
    }

    /** 실시간 총 투표 수 조회 */
    public long getTotalVoteCount(Long sessionId) {
        List<Object> values = redisTemplate.opsForHash()
                .values(String.format(VOTE_COUNT_KEY, sessionId));
        return values.stream().mapToLong(value -> Long.parseLong(value.toString())).sum();
    }

    private List<String> sessionKeys(Long sessionId) {
        return List.of(String.format(VOTE_COUNT_KEY, sessionId),
                String.format(REVISION_KEY, sessionId),
                String.format(CLOSED_KEY, sessionId), DIRTY_SESSIONS_KEY);
    }

    private static <T> DefaultRedisScript<T> script(String file, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/" + file));
        script.setResultType(resultType);
        return script;
    }
}
