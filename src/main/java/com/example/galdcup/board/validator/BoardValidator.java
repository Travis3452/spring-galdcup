package com.example.galdcup.board.validator;

import com.example.galdcup.board.domain.*;
import com.example.galdcup.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 게시판 관련 비즈니스 규칙 및 권한의 유효성을 검증하는 컴포넌트.
 */
@Component
@RequiredArgsConstructor
public class BoardValidator {

    private final BoardRepository boardRepository;
    private final BoardManagerRequestRepository boardManagerRequestRepository;

    /** 기본 조회 */
    public Board findByIdOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판입니다."));
    }

    /** OPEN 상태인 게시판을 조회하며, 아닐 경우 예외 발생 */
    public Board findActiveBoardByIdOrThrow(Long boardId) {
        Board board = this.findByIdOrThrow(boardId);

        if (board.getStatus() != Board.Status.OPEN) {
            throw new IllegalStateException("현재 폐쇄된 게시판입니다.");
        }

        return board;
    }

    /** 게시판 정책까지 통합 조회 */
    public Board findBoardWithFullPolicyOrThrow(Long boardId) {
        return boardRepository.findBoardWithFullPolicyByBoardId(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판 정보를 불러올 수 없습니다."));
    }

    /** 관리자/서브관리자 통합 권한 확인 */
    public Board getBoardIfManager(Long boardId, Long userId) {
        Board board = this.findBoardWithFullPolicyOrThrow(boardId);
        BoardPolicy policy = board.getBoardPolicy();

        boolean isMain = policy.getBoardManager() != null &&
                policy.getBoardManager().getId().equals(userId);

        boolean isSub = policy.getSubManagers().stream()
                .anyMatch(u -> u.getId().equals(userId));

        if (!isMain && !isSub) {
            throw new AccessDeniedException("해당 게시판의 관리 권한이 필요합니다.");
        }
        return board;
    }

    /** 게시판 관리자 권한 확인 */
    public Board getBoardIfBoardManager(Long boardId, Long userId) {
        Board board = this.findBoardWithFullPolicyOrThrow(boardId);
        User boardManager = board.getBoardPolicy().getBoardManager();

        if (boardManager == null || !boardManager.getId().equals(userId)) {
            throw new AccessDeniedException("해당 게시판의 관리자 권한이 필요합니다.");
        }
        return board;
    }

    /** applyForManager에서 사용 */
    public boolean hasAnySubManager(Board board) {
        return !board.getBoardPolicy().getSubManagers().isEmpty();
    }

    public void validateNoPendingRequest(Long applicantId, Long boardId) {
        if (boardManagerRequestRepository.existsByApplicantIdAndBoardIdAndStatus(
                applicantId, boardId, BoardManagerRequest.Status.PENDING)) {
            throw new IllegalStateException("이미 처리를 기다리고 있는 신청 내역이 있습니다.");
        }
    }

    public Board findDeatilById(Long boardId) {
        return boardRepository.findDetailById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));
    }
}