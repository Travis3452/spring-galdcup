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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    /**
     * 게시판 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<BoardDto>> getBoards(Pageable pageable) {
        Page<BoardDto> boards = boardService.findAll(pageable);
        return ResponseEntity.ok(boards);
    }

    /**
     * 게시판 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoard(@PathVariable Long id) {
        return boardService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 게시판 생성
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public BoardDto createBoard(@RequestBody CreateBoardRequest request,
                                @AuthenticationPrincipal CustomUserDetails principal) {
        return boardService.create(request.topic(), request.description(), principal.getId());
    }

    /**
     * 게시판 상태 변경
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<BoardDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardDto updated = boardService.updateStatus(id, request.status(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /**
     * 게시판 삭제
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boardService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}