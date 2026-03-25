package com.example.galdcup.board.validator;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.board.domain.BoardManagerRequest;
import com.example.galdcup.board.domain.BoardManagerRequestRepository;
import com.example.galdcup.board.domain.BoardRepository;
import com.example.galdcup.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 게시판 관련 비즈니스 규칙 및 권한의 유효성을 검증하는 컴포넌트.
 */
@Component
@RequiredArgsConstructor
public class BoardValidator {

    private final BoardRepository boardRepository;
    private final BoardManagerRequestRepository boardManagerRequestRepository;

    /** 게시판 존재 여부를 확인하고 게시판 엔티티를 반환. */
    public Board findByIdOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판입니다."));
    }

    /** 게시판 존재 여부를 확인하고 게시판 정책을 포함하여 게시판 엔티티를 반환. */
    public Board findBoardWithPolicyById(Long boardId) {
        return boardRepository.findBoardWithPolicyById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판 정보를 불러올 수 없습니다."));
    }

    /** 게시판이 OPEN 상태인지 확인 후 반환. */
    public Board getBoardIfOpen(Long boardId) {
        Board board = this.findByIdOrThrow(boardId);

        if (board.getStatus() == Board.Status.CLOSED) {
            throw new IllegalArgumentException("이미 폐쇄된 게시판입니다.");
        }
        return board;
    }

    /** 게시판 관리자 또는 서브 매니저 권한이 있는지 확인하고 게시판 엔티티 반환.
     * @throws AccessDeniedException 관리 권한이 없는 경우
     */
    public Board getBoardIfManager(Long boardId, Long userId) {
        Board board = this.findByIdOrThrow(boardId);
        User mainManager = board.getBoardPolicy().getBoardManager();
        List<User> subManagers = board.getBoardPolicy().getSubManagers();

        boolean isSub = subManagers.stream().anyMatch(user -> user.getId().equals(userId));
        boolean isMain = mainManager != null && mainManager.getId().equals(userId);

        if (!isSub && !isMain) {
            throw new AccessDeniedException("해당 게시판의 운영진 권한이 필요합니다.");
        }

        return board;
    }

    /** 게시판 관리자 권한을 확인하고 게시판 엔티티 반환. */
    public Board getBoardIfBoardManager(Long boardId, Long userId) {
        Board board = this.findByIdOrThrow(boardId);
        User boardManager = board.getBoardPolicy().getBoardManager();

        if (boardManager == null || !boardManager.getId().equals(userId)) {
            throw new AccessDeniedException("게시판 소유자 권한이 필요합니다.");
        }

        return board;
    }

    public boolean hasAnySubManager(Board board) {
        return !board.getBoardPolicy().getSubManagers().isEmpty();
    }

    /** 중복 신청 방지를 위해 대기 중인 매니저 신청 건이 있는지 확인. */
    public void validateNoPendingRequest(Long applicantId, Long boardId) {
        boolean exists = boardManagerRequestRepository.existsByApplicantIdAndBoardIdAndStatus(
                applicantId, boardId, BoardManagerRequest.Status.PENDING);

        if (exists) {
            throw new IllegalStateException("이미 처리를 기다리고 있는 신청 내역이 있습니다.");
        }
    }
}