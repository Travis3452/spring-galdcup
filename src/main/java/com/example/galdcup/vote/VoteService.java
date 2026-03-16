package com.example.galdcup.vote;

import com.example.galdcup.user.validator.UserValidator;
import com.example.galdcup.vote.dto.VoteDto;
import com.example.galdcup.voteSession.VoteSession;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteSessionValidator voteSessionValidator;
    private final UserValidator userValidator;
    private final SimpMessagingTemplate messagingTemplate;

    private final VoteRedisManager voteRedisManager;

    /** 투표 생성 */
    @Transactional
    public VoteDto createVote(Long voteSessionId, Long userId, int selectedOptionIndex) {
        VoteSession session = voteSessionValidator.validateAndGetVoteSession(voteSessionId);
        userValidator.findByIdOrThrow(userId);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        voteSessionValidator.validateVote(session, selectedOptionIndex, now);

        OffsetDateTime expireAt = session.getEndTime().plusDays(1);
        long ttlSeconds = ChronoUnit.SECONDS.between(now, expireAt);
        Vote vote = Vote.of(voteSessionId, userId, selectedOptionIndex, ttlSeconds);

        voteRedisManager.castVote(vote);
        broadcastVoteResults(session.getId());

        return VoteDto.from(vote);
    }

    /**
     * Redis에서 해당 세션의 모든 투표 카운트를 읽어와 WebSocket으로 Broadcast
     */
    private void broadcastVoteResults(Long voteSessionId) {
        Map<Object, Object> results = voteRedisManager.getVoteCounts(voteSessionId);
        messagingTemplate.convertAndSend("/topic/votes/" + voteSessionId, results);
    }
}