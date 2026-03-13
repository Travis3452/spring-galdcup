package com.example.galdcup.voteSession;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.event.BoardChangedEvent;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.vote.VoteOption;
import com.example.galdcup.vote.VoteOptionRepository;
import com.example.galdcup.vote.dto.VoteOptionDto;
import com.example.galdcup.voteSession.dto.CreateVoteSessionRequest;
import com.example.galdcup.voteSession.dto.VoteSessionDto;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class VoteSessionService {

    private final VoteSessionRepository voteSessionRepository;
    private final VoteOptionRepository voteOptionRepository;

    private final BoardValidator boardValidator;
    private final VoteSessionValidator voteSessionValidator;

    private final StringRedisTemplate redisTemplate;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 투표 세션 생성
     */
    @Transactional
    public VoteSessionDto createVoteSession(Long boardId, Long adminId, CreateVoteSessionRequest request) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, adminId);

        voteSessionValidator.validateNoActiveVoteSession(board);

        VoteSession voteSession = voteSessionValidator.validateAndCreateVoteSession(board, request);

        voteSession.setBoard(board);
        board.getVoteSessions().add(voteSession);

        VoteSession saved = voteSessionRepository.save(voteSession);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));

        return VoteSessionDto.from(saved);
    }

    /**
     * 게시판의 현재 진행 중인 투표 세션 조회
     */
    @Transactional(readOnly = true)
    public Optional<VoteSessionDto> getActiveVoteSession(Long boardId) {
        return voteSessionRepository.findByBoardIdAndIsFinishedFalse(boardId)
                .map(voteSession -> {
                    String hashKey = "voteSession:count:" + voteSession.getId();
                    Map<Object, Object> votes = redisTemplate.opsForHash().entries(hashKey);

                    List<VoteOptionDto> voteOptionDtos = IntStream.range(0, voteSession.getOptions().size())
                            .mapToObj(i -> {
                                VoteOption opt = voteSession.getOptions().get(i);
                                Object redisValue = votes.get(String.valueOf(i));
                                Long count = (redisValue != null) ? Long.parseLong(redisValue.toString()) : opt.getCount();
                                return new VoteOptionDto(opt.getLabel(), opt.getImageUrl(), count);
                            })
                            .toList();

                    return new VoteSessionDto(
                            voteSession.getId(),
                            boardId,
                            voteSession.getStartTime(),
                            voteSession.getEndTime(),
                            voteOptionDtos
                    );
                });
    }

    /**
     * 게시판의 종료된 투표 세션 조회
     */
    @Transactional(readOnly = true)
    public Page<VoteSessionDto> getPastVoteSessions(Long boardId, Pageable pageable) {
        Board board = boardValidator.getBoardIfOpen(boardId);

        Page<VoteSession> voteSessionPage = voteSessionRepository.findByBoardAndIsFinishedTrue(board, pageable);

        return voteSessionPage.map(session -> {
            List<VoteOptionDto> optionDtos = session.getOptions().stream()
                    .map(opt -> new VoteOptionDto(
                            opt.getLabel(),
                            opt.getImageUrl(),
                            opt.getCount()
                    ))
                    .toList();

            return new VoteSessionDto(
                    session.getId(),
                    board.getId(),
                    session.getStartTime(),
                    session.getEndTime(),
                    optionDtos
            );
        });
    }

    /**
     * 투표 세션 종료 처리
     */
    @Transactional
    public void finishVoteSession(Long boardId, Long voteSessionId, Long userId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, userId);

        VoteSession session = voteSessionValidator.validateAndGetVoteSession(voteSessionId);
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        session.setEndTime(now);

        String countKey = "voteSession:count:" + voteSessionId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(countKey);

        if (!entries.isEmpty()) {
            entries.forEach((optionIndexObj, countObj) -> {
                int selectedOptionIndex = Integer.parseInt(optionIndexObj.toString());
                long voteCount = Long.parseLong(countObj.toString());

                if (selectedOptionIndex >= 0 && selectedOptionIndex < session.getOptions().size()) {
                    Long optionId = session.getOptions().get(selectedOptionIndex).getId();
                    voteOptionRepository.incrementVoteCount(optionId, voteCount);
                }
            });

            redisTemplate.delete(countKey);
        }

        session.setFinished(true);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));
    }
}