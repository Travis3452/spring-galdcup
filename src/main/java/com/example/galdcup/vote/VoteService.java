package com.example.galdcup.vote;

import com.example.galdcup.user.validator.UserValidator;
import com.example.galdcup.vote.dto.VoteDto;
import com.example.galdcup.voteSession.VoteSession;
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

    /** 투표 생성 */
    @Transactional
    public VoteDto createVote(Long voteSessionId, Long userId, int selectedOptionIndex) {
        VoteSession session = voteSessionValidator.validateAndGetVoteSession(voteSessionId);
        userValidator.findByIdOrThrow(userId);

        // 중복 투표 방지
        String redisKey = "vote:" + voteSessionId + ":" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            throw new IllegalStateException("이미 해당 게시판에 투표하였습니다.");
        }

        // 투표 시간 검증
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime())) {
            throw new IllegalStateException("현재는 투표 가능 시간이 아닙니다.");
        }

        // 옵션 인덱스 검증
        if (selectedOptionIndex < 0 || selectedOptionIndex >= session.getOptions().size()) {
            throw new IllegalArgumentException("잘못된 투표 옵션입니다.");
        }

        OffsetDateTime expireAt = session.getEndTime().plusDays(1);
        long ttl = ChronoUnit.SECONDS.between(OffsetDateTime.now(), expireAt);

        Vote vote = Vote.of(voteSessionId, userId, selectedOptionIndex, ttl);
        redisTemplate.opsForValue().set(redisKey, vote, ttl, TimeUnit.SECONDS);

        // Redis 카운트 증가
        String countKey = "voteSession:count:" + session.getId();
        redisTemplate.opsForHash().increment(countKey, String.valueOf(selectedOptionIndex), 1);

        // 실시간 브로드캐스트 로직 추가
        broadcastVoteResults(session.getId());

        return VoteDto.from(vote);
    }

    /**
     * Redis에서 해당 세션의 모든 투표 카운트를 읽어와 WebSocket으로 Broadcast
     */
    private void broadcastVoteResults(Long voteSessionId) {
        String countKey = "voteSession:count:" + voteSessionId;

        Map<Object, Object> results = redisTemplate.opsForHash().entries(countKey);

        messagingTemplate.convertAndSend("/topic/votes/" + voteSessionId, results);
    }
}