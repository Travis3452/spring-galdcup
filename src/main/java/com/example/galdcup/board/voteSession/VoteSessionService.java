package com.example.galdcup.board.voteSession;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.common.redis.CachedPageResponse;
import com.example.galdcup.board.vote.domain.VoteOption;
import com.example.galdcup.board.vote.redis.VoteRedisManager;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.domain.VoteSessionRepository;
import com.example.galdcup.board.voteSession.redis.VoteSessionRedisManager;
import com.example.galdcup.board.voteSession.request.CreateVoteSessionRequest;
import com.example.galdcup.board.voteSession.response.VoteSessionDto;
import com.example.galdcup.board.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteSessionService {

    private final VoteSessionRepository voteSessionRepository;
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
                board, request.topic(), request.description(), request.startTime(), request.endTime(), options);

        voteSessionRepository.save(voteSession);

        // 진행 중인 세션 캐시 무효화
        voteSessionRedisManager.deleteLatestVoteSession(boardId);

        return VoteSessionDto.from(voteSession);
    }

    /** 게시판의 최신 투표 세션 조회 */
    @Transactional(readOnly = true)
    public Optional<VoteSessionDto> getLatestVoteSession(Long boardId) {
        Optional<VoteSessionDto> sessionDtoOpt = voteSessionRedisManager.getLatestVoteSession(boardId);

        if (sessionDtoOpt.isEmpty()) {
            sessionDtoOpt = voteSessionRepository.findTopByBoardIdOrderByEndTimeDesc(boardId)
                    .map(session -> {
                        VoteSessionDto dto = VoteSessionDto.from(session);
                        voteSessionRedisManager.saveLatestVoteSession(boardId, dto);
                        return dto;
                    });
        }

        // 진행 중인 투표의 경우, Redis의 실시간 투표 데이터를 합산하여 totalVotes 갱신
        sessionDtoOpt
                .filter(VoteSessionDto::isActive)
                .ifPresent(dto -> {
                    Long realTimeTotal = voteRedisManager.getTotalVoteCount(dto.getId());
                    dto.setTotalVotes(realTimeTotal);
                });

        return sessionDtoOpt;
    }

    /** 종료된 투표 세션 목록 조회 (캐싱) */
    @Transactional(readOnly = true)
    public Page<VoteSessionDto> getPastVoteSessions(Long boardId, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        // Redis에서 해당 페이지 캐시 조회
        Optional<CachedPageResponse<VoteSessionDto>> cached =
                voteSessionRedisManager.getPastVoteSessions(boardId, page, size);

        if (cached.isPresent()) {
            return cached.get().toPage(pageable);
        }

        // 캐시가 없으면 DB 조회
        Page<VoteSessionDto> dtoPage = fetchPastSessionsFromDb(boardId, pageable);

        // 조회된 페이지를 캐시에 저장
        voteSessionRedisManager.savePastVoteSessions(boardId, page, size, CachedPageResponse.of(dtoPage));

        return dtoPage;
    }

    /** 관리자에 의한 투표 세션 수동 종료 */
    @Transactional
    public void finishVoteSession(Long boardId, Long voteSessionId, Long userId) {
        boardValidator.getBoardIfBoardManager(boardId, userId);
        VoteSession session = voteSessionValidator.validateAndGetVoteSessionWithOptions(voteSessionId);

        session.terminate();

        // Redis 카운트를 DB로 동기화
        syncRedisVotesToDb(session);

        // 기존 캐시 무효화
        voteRedisManager.deleteVoteCounts(voteSessionId);
        voteSessionRedisManager.deleteLatestVoteSession(boardId);
        voteSessionRedisManager.deletePastVoteSessions(boardId);
    }

    /** DB에서 과거 세션 데이터를 가져오는 헬퍼 메서드 */
    private Page<VoteSessionDto> fetchPastSessionsFromDb(Long boardId, Pageable pageable) {
        Board board = boardValidator.findActiveBoardByIdOrThrow(boardId);

        return voteSessionRepository.findByBoardAndIsFinishedTrue(board, pageable)
                .map(VoteSessionDto::from);
    }

    /** Redis 카운트를 DB에 동기화 */
    @Transactional
    public void syncRedisVotesToDb(VoteSession session) {
        Map<String, String> entries = voteRedisManager.getVoteCounts(session.getId());
        if (entries.isEmpty()) return;

        entries.forEach((optionIndex, countStr) -> {
            int idx = Integer.parseInt(optionIndex);
            long count = Long.parseLong(countStr);

            if (idx >= 0 && idx < session.getOptions().size()) {
                session.getOptions().get(idx).updateCount(count);
            }
        });
    }
}