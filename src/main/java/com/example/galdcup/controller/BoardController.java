package com.example.galdcup.controller;

import com.example.galdcup.dto.board.*;
import com.example.galdcup.entity.Board;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Board saved = boardService.create(request.topicId(), principal.getUsername());
        return ResponseEntity.created(URI.create("/api/boards/" + saved.getId()))
                .body(BoardDto.from(saved));
    }

    // 게시판 상태 수정
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BoardDto> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Board updated = boardService.updateStatus(id, principal.getUsername(), request.status());
        return ResponseEntity.ok(BoardDto.from(updated));
    }

    // 게시판 삭제
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boardService.delete(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}