package com.example.galdcup.board.voteSession;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.common.redis.CachedPageResponse;
import com.example.galdcup.board.vote.domain.VoteOption;
import com.example.galdcup.board.vote.redis.VoteRedisManager;
import com.example.galdcup.board.vote.redis.VoteCountSnapshot;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.OffsetDateTime;
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
        VoteSession session = findForUpdate(voteSessionId);
        Long actualBoardId = session.getBoard().getId();
        if (!actualBoardId.equals(boardId)) {
            throw new AccessDeniedException(
                    "해당 게시판에 속하지 않는 투표 세션입니다.");
        }
        completeSession(session);
    }

    /** DB에서 과거 세션 데이터를 가져오는 헬퍼 메서드 */
    private Page<VoteSessionDto> fetchPastSessionsFromDb(Long boardId, Pageable pageable) {
        Board board = boardValidator.findActiveBoardByIdOrThrow(boardId);

        return voteSessionRepository.findByBoardAndIsFinishedTrue(board, pageable)
                .map(VoteSessionDto::from);
    }

    /** ID로 다시 조회한 영속 엔티티를 갱신. 작업 간 충돌은 세션 행 잠금으로 방지 */
    @Transactional
    public void syncRedisVotesToDb(Long sessionId) {
        VoteSession session = findForUpdate(sessionId);
        if (session.isFinished() || !session.getEndTime().isAfter(OffsetDateTime.now())) {
            completeSession(session);
            return;
        }

        VoteCountSnapshot snapshot = voteRedisManager.getSnapshot(sessionId);
        if (snapshot.closedAtEpochMilli() != null) {
            // 종료 요청 후 DB 롤백 또는 프로세스 종료가 있었던 경우 종료 작업 복구
            completeSession(session);
            return;
        }
        applyVoteCounts(session, snapshot.counts());
        acknowledgeAfterCommit(session, snapshot, false);
    }

    /** 스케줄러의 외부 호출마다 별도 트랜잭션을 사용하여 세션별 실패를 격리 */
    @Transactional
    public void finishExpiredVoteSession(Long sessionId) {
        VoteSession session = findForUpdate(sessionId);
        if (!session.isFinished() && session.getEndTime().isAfter(OffsetDateTime.now())) {
            return;
        }
        completeSession(session);
    }

    private VoteSession findForUpdate(Long sessionId) {
        return voteSessionRepository.findForUpdateById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("투표 세션을 찾을 수 없습니다."));
    }

    private void completeSession(VoteSession session) {
        // 접수 차단과 스냅샷을 원자적으로 수행. DB 커밋 실패 시에도 dirty 상태를 유지
        VoteCountSnapshot snapshot = voteRedisManager.closeAndGetSnapshot(session);
        if (snapshot.closedAtEpochMilli() == null) {
            throw new IllegalStateException("투표 접수 종료를 확인할 수 없습니다.");
        }
        applyVoteCounts(session, snapshot.counts());
        session.finishAt(OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(snapshot.closedAtEpochMilli()), session.getEndTime().getOffset()));
        acknowledgeAfterCommit(session, snapshot, true);
    }

    private void applyVoteCounts(VoteSession session, Map<String, String> entries) {
        entries.forEach((optionIndex, countStr) -> {
            int index = Integer.parseInt(optionIndex);
            long count = Long.parseLong(countStr);
            if (index < 0 || index >= session.getOptions().size() || count < 0) {
                throw new IllegalStateException("유효하지 않은 투표 집계입니다.");
            }
            session.getOptions().get(index).updateCount(count);
        });
    }

    private void acknowledgeAfterCommit(VoteSession session, VoteCountSnapshot snapshot, boolean finished) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("투표 동기화에는 활성 트랜잭션이 필요합니다.");
        }
        Long sessionId = session.getId();
        Long boardId = session.getBoard().getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    if (finished) {
                        voteSessionRedisManager.deleteLatestVoteSession(boardId);
                        voteSessionRedisManager.deletePastVoteSessions(boardId);
                    }
                    // 변경 번호가 달라졌다면 새 표를 처리할 다음 동기화를 남김
                    voteRedisManager.acknowledgeSnapshot(sessionId, snapshot, finished);
                } catch (RuntimeException e) {
                    // DB는 이미 커밋됨. dirty 항목과 집계를 남겨 다음 스케줄에서 재시도
                    log.error("투표 DB 반영 후 Redis 정리 실패 - 세션 ID: {}", sessionId, e);
                }
            }
        });
    }
}
