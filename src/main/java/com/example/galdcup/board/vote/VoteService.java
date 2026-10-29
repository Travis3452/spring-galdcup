package com.example.galdcup.board.vote;

import com.example.galdcup.user.validator.UserValidator;
import com.example.galdcup.board.vote.domain.Vote;
import com.example.galdcup.board.vote.redis.VoteRedisManager;
import com.example.galdcup.board.vote.response.VoteDto;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

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
    @Transactional(readOnly = true)
    public VoteDto createVote(Long boardId, Long voteSessionId, Long userId, int selectedOptionIndex) {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        VoteSession session = voteSessionValidator.validateAndGetVotingSession(
                boardId, voteSessionId, selectedOptionIndex, now);
        userValidator.findByIdOrThrow(userId);

        OffsetDateTime expireAt = session.getEndTime().plusDays(1);
        long ttlSeconds = ChronoUnit.SECONDS.between(now, expireAt);
        Vote vote = Vote.of(session.getId(), userId, selectedOptionIndex, ttlSeconds);

        voteRedisManager.castVote(vote, session);

        try {
            broadcastVoteResults(session.getId());
        } catch (RuntimeException e) {
            // 기록은 이미 성공했으므로 알림 실패로 사용자에게 재시도를 요구하지 않음
            log.warn("투표 기록 후 실시간 알림 실패 - 세션 ID: {}", session.getId(), e);
        }

        return VoteDto.from(vote);
    }

    /** Redis에서 현재 누적된 전체 투표 카운트를 읽어와 총 투표수로 합산한 뒤 WebSocket으로 전송 */
    private void broadcastVoteResults(Long voteSessionId) {
        long totalVotes = voteRedisManager.getTotalVoteCount(voteSessionId);

        messagingTemplate.convertAndSend("/topic/votes/" + voteSessionId, totalVotes);
    }
}
