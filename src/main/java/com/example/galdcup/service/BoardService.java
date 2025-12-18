package com.example.galdcup.service;

import com.example.galdcup.entity.Board;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.BoardRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    // 전체 게시판 조회
    @Transactional(readOnly = true)
    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    // 특정 게시판 조회
    @Transactional(readOnly = true)
    public Optional<Board> findById(Long id) {
        return boardRepository.findById(id);
    }

    // 게시판 생성
    @Transactional
    public Board create(String topic, String adminOauthId) {
        User admin = userRepository.findByOauthId(adminOauthId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        Board board = Board.builder()
                .topic(topic)
                .admin(admin)
                .status(Board.Status.OPEN)
                .build();

        return boardRepository.save(board);
    }

    // 게시판 상태 변경
    @Transactional
    public Board updateStatus(Long boardId, Board.Status newStatus, String currentUserOauthId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        if (!board.getAdmin().getOauthId().equals(currentUserOauthId)) {
            throw new AccessDeniedException("이 게시판의 관리자가 아닙니다.");
        }

        board.setStatus(newStatus);
        return boardRepository.save(board);
    }

    // 게시판 삭제
    @Transactional
    public void delete(Long id, String oauthId) {
        User admin = userRepository.findByOauthId(oauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        boardRepository.delete(board);
    }
}