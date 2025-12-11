package com.example.galdcup.service;

import com.example.galdcup.entity.Board;
import com.example.galdcup.entity.GaldcupTopic;
import com.example.galdcup.entity.User;
import com.example.galdcup.entity.User.Role;
import com.example.galdcup.repository.BoardRepository;
import com.example.galdcup.repository.GaldcupTopicRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final GaldcupTopicRepository galdcupTopicRepository;
    private final UserRepository userRepository;

    // 전체 게시판 조회
    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    // 특정 게시판 조회
    public Optional<Board> findById(Long id) {
        return boardRepository.findById(id);
    }

    // 게시판 생성
    public Board create(Long galdcupTopicId, String oauthId) {
        User admin = userRepository.findByOauthId(oauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new SecurityException("ADMIN 권한이 있어야 게시판을 생성할 수 있습니다.");
        }

        GaldcupTopic galdcupTopic = galdcupTopicRepository.findById(galdcupTopicId)
                .orElseThrow(() -> new IllegalArgumentException("GaldcupTopic을 찾을 수 없습니다."));

        Board board = Board.builder()
                .galdcupTopic(galdcupTopic)
                .admin(admin)
                .status(Board.Status.OPEN)
                .build();

        return boardRepository.save(board);
    }

    // 게시판 상태 수정
    public Board updateStatus(Long id, String oauthId, Board.Status status) {
        User admin = userRepository.findByOauthId(oauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new SecurityException("ADMIN 권한이 있어야 게시판을 수정할 수 있습니다.");
        }

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        board.setStatus(status);
        return boardRepository.save(board);
    }

    // 게시판 삭제 (ADMIN만 가능)
    public void delete(Long id, String oauthId) {
        User admin = userRepository.findByOauthId(oauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new SecurityException("ADMIN 권한이 있어야 게시판을 삭제할 수 있습니다.");
        }

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        boardRepository.delete(board);
    }
}