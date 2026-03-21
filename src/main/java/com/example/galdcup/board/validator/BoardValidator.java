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

@Component
@RequiredArgsConstructor
public class BoardValidator {
    private final BoardRepository boardRepository;
    private final BoardManagerRequestRepository boardManagerRequestRepository;
    
    /**
     * 게시판이 존재하는지 검증
     */
    public Board findByIdOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));
    }

    /**
     * 게시판이 존재하는지 검증
     */
    public Board findBoardWithPolicyById(Long boardId) {
        return boardRepository.findBoardWithPolicyById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));
    }


    /**
     * 게시판이 존재하고 OPEN 상태인지 검증
     */
    public Board getBoardIfOpen(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        if (board.getStatus() == Board.Status.CLOSED) {
            throw new IllegalArgumentException("닫힌 게시판입니다.");
        }
        return board;
    }

    /**
     * 관리자 권한 여부 체크
     */
    public Board getBoardIfManager(Long boardId, Long userId) {
        Board board = this.findByIdOrThrow(boardId);
        User boardManager = board.getBoardPolicy().getBoardManager();
        List<User> subManagers = board.getBoardPolicy().getSubManagers();

        if (subManagers.stream().noneMatch(user -> user.getId().equals(userId))
                && !boardManager.getId().equals(userId)) {
            throw new AccessDeniedException("게시판 관리자 권한이 필요합니다.");
        }

        return board;
    }

    /**
     * 게시판 관리자 여부 체크
     */
    public Board getBoardIfBoardManager(Long boardId, Long userId) {
        Board board = this.findByIdOrThrow(boardId);
        User boardManager = board.getBoardPolicy().getBoardManager();

        if (!boardManager.getId().equals(userId)) {
            throw new AccessDeniedException("게시판 관리자 권한이 필요합니다.");
        }

        return board;
    }

    /**
     * 해당 게시판에 서브 매니저가 한 명이라도 존재하는지 확인
     */
    public boolean hasAnySubManager(Board board) {
        return !board.getBoardPolicy().getSubManagers().isEmpty();
    }

    /**
     * 이미 해당 게시판에 대기 중인 신청이 있는지 검증
     */
    public void validateNoPendingRequest(Long applicantId, Long boardId) {
        boolean exists = boardManagerRequestRepository.existsByApplicantIdAndBoardIdAndStatus(
                applicantId, boardId, BoardManagerRequest.Status.PENDING);

        if (exists) {
            throw new IllegalStateException("이미 해당 게시판에 승인 대기 중인 신청이 있습니다.");
        }
    }
}