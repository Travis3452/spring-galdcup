package com.example.galdcup.controller;

import com.example.galdcup.dto.board.*;
import com.example.galdcup.entity.Board;
import com.example.galdcup.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 전체 게시판 조회
    @GetMapping
    public ResponseEntity<List<BoardDto>> getBoards() {
        List<BoardDto> boards = boardService.findAll()
                .stream()
                .map(BoardDto::from)
                .toList();
        return ResponseEntity.ok(boards);
    }

    // 특정 게시판 조회
    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoard(@PathVariable Long id) {
        Optional<Board> boardOpt = boardService.findById(id);
        return boardOpt.map(b -> ResponseEntity.ok(BoardDto.from(b)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 게시판 생성
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(@Valid @RequestBody CreateBoardRequest request) {
        Board saved = boardService.create(request.topicId(), request.authorId());
        return ResponseEntity.created(URI.create("/api/boards/" + saved.getId()))
                .body(BoardDto.from(saved));
    }

    // 게시판 상태 수정 (OPEN ↔ CLOSED)
    @PutMapping("/{id}")
    public ResponseEntity<BoardDto> updateBoard(@PathVariable Long id,
                                                @Valid @RequestBody UpdateBoardRequest request) {
        Board updated = boardService.updateStatus(id, request.status());
        return ResponseEntity.ok(BoardDto.from(updated));
    }

    // 게시판 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        boardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}