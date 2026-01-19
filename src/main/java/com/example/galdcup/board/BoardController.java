package com.example.galdcup.board;

import com.example.galdcup.board.dto.*;
import com.example.galdcup.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    /** 게시판 목록 조회 */
    @GetMapping
    public ResponseEntity<Page<BoardDto>> getBoards(Pageable pageable) {
        Page<BoardDto> boards = boardService.findAll(pageable);
        return ResponseEntity.ok(boards);
    }

    /** 인기 게시판 목록 조회 */
    @GetMapping("/popular")
    public ResponseEntity<List<BoardDto>> getPopularBoards(Pageable pageable) {
        List<BoardDto> boards = boardService.getPopularBoards();
        return ResponseEntity.ok(boards);
    }

    /** 게시판 단건 조회 */
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDto> getBoard(@PathVariable Long boardId) {
        return boardService.findById(boardId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 게시판 검색 */
    @GetMapping("/search")
    public ResponseEntity<Page<BoardDto>> searchBoards(Pageable pageable,
                                                       @RequestParam String keyword) {
        Page<BoardDto> boards = boardService.getBoardsByKeyword(pageable, keyword);
        return ResponseEntity.ok(boards);
    }
    
    /** 게시판 정책 조회 */
    @GetMapping("/{boardId}/policy")
    public ResponseEntity<BoardPolicyDto> getBoardPolicy(@PathVariable Long boardId) {
        return boardService.findPolicyByBoardId(boardId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 게시판 정책 업데이트 (boardManager만 접근 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{boardId}/policy")
    public ResponseEntity<BoardPolicyDto> updateBoardPolicy(
            @PathVariable Long boardId,
            @Valid @RequestBody UpdateBoardPolicyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardService.updatePolicy(boardId, request, principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 게시판 생성 */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardDto created = boardService.create(request.topic(), request.description(), principal.getId());
        return ResponseEntity.ok(created);
    }

    /** 게시판 상태 변경 */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{boardId}/status")
    public ResponseEntity<BoardDto> updateStatus(
            @PathVariable Long boardId,
            @Valid @RequestBody UpdateBoardRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardDto updated = boardService.updateStatus(boardId, request.status(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 게시판 삭제 */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boardService.delete(boardId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /** 서브 매니저 추가 (boardManager만 접근 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{boardId}/policy/sub-managers")
    public ResponseEntity<BoardPolicyDto> addSubManager(
            @PathVariable Long boardId,
            @Valid @RequestBody SubManagerRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardService.addSubManager(boardId, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 서브 매니저 삭제 (boardManager만 접근 가능) */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{boardId}/policy/sub-managers")
    public ResponseEntity<BoardPolicyDto> removeSubManager(
            @PathVariable Long boardId,
            @Valid @RequestBody SubManagerRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardService.removeSubManager(boardId, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

}