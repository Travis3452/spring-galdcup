package com.example.galdcup.board.validator;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardValidator {
    private final BoardRepository boardRepository;
    
    /**
     * 게시판이 존재하는지 검증
     */
    public Board validateAndGetBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));
    }
    
    /**
     * 게시판이 존재하고 OPEN 상태인지 검증
     */
    public Board validateAndGetActiveBoard(Long boardId) {
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
    public void checkManagerAuthority(Board board, Long userId) {
        User boardManager = board.getBoardPolicy().getBoardManager();
        List<User> subManagers = board.getBoardPolicy().getSubManagers();

        if (subManagers.stream().noneMatch(user -> user.getId().equals(userId))
                && !boardManager.getId().equals(userId)) {
            throw new AccessDeniedException("게시판 관리자 권한이 필요합니다.");
        }
    }

    /**
     * 게시판 관리자 여부 체크
     */
    public void checkBoardManagerAuthority(Board board, Long userId) {
        User boardManager = board.getBoardPolicy().getBoardManager();

        if (!boardManager.getId().equals(userId)) {
            throw new AccessDeniedException("게시판 관리자 권한이 필요합니다.");
        }
    }
}