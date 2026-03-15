package com.example.galdcup.vote;

import com.example.galdcup.user.validator.UserValidator;
import com.example.galdcup.vote.dto.VoteDto;
import com.example.galdcup.voteSession.VoteSession;
import com.example.galdcup.voteSession.VoteSessionRedisManager;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final VoteSessionValidator voteSessionValidator;
    private final UserValidator userValidator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private final VoteSessionRedisManager voteSessionRedisManager;

    /** 투표 생성 */
    @Transactional
    public VoteDto createVote(Long voteSessionId, Long userId, int selectedOptionIndex) {
        VoteSession session = voteSessionValidator.validateAndGetVoteSession(voteSessionId);
        userValidator.findByIdOrThrow(userId);

        String redisKey = "galdcup:vote-sessions:" + voteSessionId + ":user:" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            throw new IllegalStateException("이미 해당 게시판에 투표하였습니다.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime())) {
            throw new IllegalStateException("현재는 투표 가능 시간이 아닙니다.");
        }

        if (selectedOptionIndex < 0 || selectedOptionIndex >= session.getOptions().size()) {
            throw new IllegalArgumentException("잘못된 투표 옵션입니다.");
        }

        OffsetDateTime expireAt = session.getEndTime().plusDays(1);
        long ttl = ChronoUnit.SECONDS.between(OffsetDateTime.now(), expireAt);

        Vote vote = Vote.of(voteSessionId, userId, selectedOptionIndex, ttl);
        redisTemplate.opsForValue().set(redisKey, vote, ttl, TimeUnit.SECONDS);

        voteSessionRedisManager.incrementVoteCount(session.getId(), selectedOptionIndex);

        broadcastVoteResults(session.getId());

        return VoteDto.from(vote);
    }

    /**
     * Redis에서 해당 세션의 모든 투표 카운트를 읽어와 WebSocket으로 Broadcast
     */
    private void broadcastVoteResults(Long voteSessionId) {
        Map<Object, Object> results = voteSessionRedisManager.getVoteCounts(voteSessionId);

        messagingTemplate.convertAndSend("/topic/votes/" + voteSessionId, results);
    }
}