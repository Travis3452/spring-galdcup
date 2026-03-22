package com.example.galdcup.voteSession;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.common.redis.CachedPageResponse;
import com.example.galdcup.vote.domain.VoteOption;
import com.example.galdcup.vote.domain.VoteOptionRepository;
import com.example.galdcup.vote.redis.VoteRedisManager;
import com.example.galdcup.vote.response.VoteOptionDto;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.domain.VoteSessionRepository;
import com.example.galdcup.voteSession.redis.VoteSessionRedisManager;
import com.example.galdcup.voteSession.request.CreateVoteSessionRequest;
import com.example.galdcup.voteSession.response.VoteSessionDto;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteSessionService {

    private final VoteSessionRepository voteSessionRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final BoardValidator boardValidator;
    private final VoteSessionValidator voteSessionValidator;
    private final VoteSessionRedisManager voteSessionRedisManager;
    private final VoteRedisManager voteRedisManager;

    /** 투표 세션 생성 */
    @Transactional
    public VoteSessionDto createVoteSession(Long boardId, Long adminId, CreateVoteSessionRequest request) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, adminId);
        voteSessionValidator.validateNoActiveVoteSession(board);

        List<VoteOption> options = request.options().stream()
                .map(opt -> VoteOption.create(opt.label(), opt.imageUrl()))
                .toList();

        VoteSession voteSession = VoteSession.create(
                board, request.startTime(), request.endTime(), options);

        voteSessionRepository.save(voteSession);
        voteSessionRedisManager.deleteVoteSession(boardId);

        return VoteSessionDto.from(voteSession);
    }

    /** 현재 진행 중인 투표 세션 조회 (실시간 Redis 합계 반영) */
    @Transactional(readOnly = true)
    public Optional<VoteSessionDto> getActiveVoteSession(Long boardId) {
        Optional<VoteSessionDto> cachedVoteSession = voteSessionRedisManager.getActiveVoteSession(boardId);

        VoteSessionDto cached;
        if (cachedVoteSession.isPresent()) {
            cached = cachedVoteSession.get();
        } else {
            Optional<VoteSession> optionalVoteSession = voteSessionRepository.findByBoardIdAndIsFinishedFalse(boardId);
            if (optionalVoteSession.isEmpty()) return Optional.empty();

            cached = VoteSessionDto.from(optionalVoteSession.get());
            voteSessionRedisManager.saveVoteSession(boardId, cached);
        }

        Map<Object, Object> realTimeCounts = voteRedisManager.getVoteCounts(cached.getId());
        return Optional.of(assembleVoteSession(cached, realTimeCounts));
    }

    /** 종료된 투표 세션 목록 조회 */
    @Transactional(readOnly = true)
    public Page<VoteSessionDto> getPastVoteSessions(Long boardId, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        Optional<CachedPageResponse<VoteSessionDto>> cached = voteSessionRedisManager.getPastVoteSessions(boardId, page, size);
        if (cached.isPresent()) {
            return cached.get().toPage(pageable);
        }

        Page<VoteSession> dbPage = voteSessionRepository.findByBoardAndIsFinishedTrue(
                boardValidator.getBoardIfOpen(boardId), pageable);

        Page<VoteSessionDto> dtoPage = dbPage.map(VoteSessionDto::from);

        voteSessionRedisManager.savePastVoteSessions(boardId, page, size, CachedPageResponse.of(dtoPage));

        return dtoPage;
    }

    /** 관리자에 의한 투표 세션 수동 종료 */
    @Transactional
    public void finishVoteSession(Long boardId, Long voteSessionId, Long userId) {
        boardValidator.getBoardIfBoardManager(boardId, userId);
        VoteSession session = voteSessionValidator.validateAndGetVoteSession(voteSessionId);

        session.terminate();

        syncRedisVotesToDb(session);

        voteRedisManager.deleteVoteCounts(voteSessionId);
        voteSessionRedisManager.deleteVoteSession(boardId);
        voteSessionRedisManager.deletePastVoteSessions(boardId);
    }

    /**
     * Redis에 쌓인 투표 카운트를 DB(VoteOption)에 최종 동기화하는 헬퍼 메서드
     */
    // VoteSessionService.java 내부
    @Transactional
    public void syncRedisVotesToDb(VoteSession session) {
        Map<Object, Object> entries = voteRedisManager.getVoteCounts(session.getId());
        if (entries.isEmpty()) return;

        entries.forEach((optionIndexObj, countObj) -> {
            int selectedOptionIndex = Integer.parseInt(optionIndexObj.toString());
            long totalCount = Long.parseLong(countObj.toString());

            if (selectedOptionIndex >= 0 && selectedOptionIndex < session.getOptions().size()) {
                VoteOption option = session.getOptions().get(selectedOptionIndex);
                option.updateCount(totalCount);
            }
        });
    }

    /**
     * 정적 데이터(DTO)와 Redis의 실시간 누적 수치를 병합하는 헬퍼.
     */
    private VoteSessionDto assembleVoteSession(VoteSessionDto cached, Map<Object, Object> counts) {
        List<VoteOptionDto> mergedOptions = IntStream.range(0, cached.getOptions().size())
                .mapToObj(i -> {
                    VoteOptionDto opt = cached.getOptions().get(i);
                    Object redisVal = counts.get(String.valueOf(i));

                    Long currentCount = (redisVal != null) ? Long.parseLong(redisVal.toString()) : opt.getCount();

                    return VoteOptionDto.builder()
                            .label(opt.getLabel())
                            .imageUrl(opt.getImageUrl())
                            .count(currentCount)
                            .build();
                })
                .toList();

        return VoteSessionDto.builder()
                .id(cached.getId())
                .boardId(cached.getBoardId())
                .startTime(cached.getStartTime())
                .endTime(cached.getEndTime())
                .options(mergedOptions)
                .build();
    }
}