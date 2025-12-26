package com.example.galdcup.controller;

import com.example.galdcup.dto.board.BoardDto;
import com.example.galdcup.dto.board.CreateBoardRequest;
import com.example.galdcup.dto.board.UpdateBoardRequest;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 게시판 생성
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardDto saved = boardService.create(request.topic(), principal.getUsername());
        return ResponseEntity.created(URI.create("/api/boards/" + saved.id()))
                .body(saved);
    }

    // 게시판 전체 조회
    @GetMapping
    public ResponseEntity<Page<BoardDto>> getBoards(Pageable pageable) {
        Page<BoardDto> boards = boardService.findAll(pageable);
        return ResponseEntity.ok(boards);
    }

    // 게시판 상태 변경
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<BoardDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardDto updated = boardService.updateStatus(id, request.status(), principal.getUsername());
        return ResponseEntity.ok(updated);
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