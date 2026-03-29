package com.example.galdcup.vote;

import com.example.galdcup.user.validator.UserValidator;
import com.example.galdcup.vote.domain.Vote;
import com.example.galdcup.vote.redis.VoteRedisManager;
import com.example.galdcup.vote.response.VoteDto;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

    private final VoteSessionValidator voteSessionValidator;
    private final UserValidator userValidator;
    private final SimpMessagingTemplate messagingTemplate;
    private final VoteRedisManager voteRedisManager;

    /**
     * 투표 생성 및 실시간 카운트 처리
     */
    @Transactional
    public VoteDto createVote(Long boardId, Long voteSessionId, Long userId, int selectedOptionIndex) {
        VoteSession session = voteSessionValidator.validateAndGetActiveVoteSession(boardId);
        userValidator.findByIdOrThrow(userId);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));

        ensureVoteCountsInRedis(session, now);

        OffsetDateTime expireAt = session.getEndTime().plusDays(1);
        long ttlSeconds = ChronoUnit.SECONDS.between(now, expireAt);
        Vote vote = Vote.of(voteSessionId, userId, selectedOptionIndex, ttlSeconds);

        voteRedisManager.castVote(vote);

        broadcastVoteResults(session.getId());

        return VoteDto.from(vote);
    }

    /**
     * Redis에 카운트 데이터가 없는 경우 DB 값을 로드하여 초기화
     */
    private void ensureVoteCountsInRedis(VoteSession session, OffsetDateTime now) {
        if (!voteRedisManager.hasVoteCounts(session.getId())) {
            Map<String, String> initialCounts = session.getOptions().stream()
                    .collect(Collectors.toMap(
                            opt -> String.valueOf(session.getOptions().indexOf(opt)),
                            opt -> String.valueOf(opt.getCount())
                    ));

            long ttl = ChronoUnit.SECONDS.between(now, session.getEndTime().plusDays(1));
            voteRedisManager.warmUpVoteCounts(session.getId(), initialCounts, Math.max(ttl, 3600));
        }
    }

    /**
     * Redis에서 현재 누적된 전체 투표 카운트를 읽어와 WebSocket으로 전송
     */
    private void broadcastVoteResults(Long voteSessionId) {
        Map<String, String> results = voteRedisManager.getVoteCounts(voteSessionId);
        messagingTemplate.convertAndSend("/topic/votes/" + voteSessionId, results);
    }
}