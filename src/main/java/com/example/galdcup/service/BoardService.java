package com.example.galdcup.service;

import com.example.galdcup.entity.Board;
import com.example.galdcup.entity.GaldcupTopic;
import com.example.galdcup.repository.BoardRepository;
import com.example.galdcup.repository.GaldcupTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final GaldcupTopicRepository topicRepository;

    // 게시판 생성
    public Board create(Long topicId, Long authorId) {
        GaldcupTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("주제를 찾을 수 없습니다."));

        Board board = Board.builder()
                .topic(topic)
                .status(Board.Status.OPEN)
                .build();

        return boardRepository.save(board);
    }

    // 모든 게시판 조회
    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    // 게시판 조회
    public Optional<Board> findById(Long id) {
        return boardRepository.findById(id);
    }

    // 게시판 상태 업데이트
    public Board updateStatus(Long id, Board.Status status) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        board.setStatus(status);
        return boardRepository.save(board);
    }

    // 게시판 삭제
    public void delete(Long id) {
        if (!boardRepository.existsById(id)) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        boardRepository.deleteById(id);
    }
}