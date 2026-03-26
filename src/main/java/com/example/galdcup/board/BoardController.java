package com.example.galdcup.board;

import com.example.galdcup.board.request.BoardRequest;
import com.example.galdcup.board.response.BoardDetailResponse;
import com.example.galdcup.board.response.BoardDto;
import com.example.galdcup.board.response.BoardManagerRequestDto;
import com.example.galdcup.board.response.BoardPolicyDto;
import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

    // ==========================================
    // 1. 게시판 조회
    // ==========================================

    /** 전체 게시판 목록을 페이징하여 조회합니다. */
    @GetMapping
    public ResponseEntity<Page<BoardDto>> getBoards(Pageable pageable) {
        Page<BoardDto> boards = boardService.findAll(pageable);
        return ResponseEntity.ok(boards);
    }

    /** 조회수 기반의 인기 게시판 목록을 조회합니다. */
    @GetMapping("/popular")
    public ResponseEntity<List<BoardDto>> getPopularBoards() {
        return ResponseEntity.ok(boardService.getPopularBoards());
    }

    /** 특정 게시판의 기본 정보 및 조회수를 확인합니다. */
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDto> getBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.findById(boardId));
    }

    /** 게시판 진입에 필요한 정책, 카테고리 등 상세 데이터를 통합 조회합니다. */
    @GetMapping("/{boardId}/details")
    public ResponseEntity<BoardDetailResponse> getBoardDetails(@PathVariable Long boardId) {
        BoardDetailResponse response = boardService.getBoardDetail(boardId);
        return ResponseEntity.ok(response);
    }

    /** 키워드를 통해 게시판을 검색합니다. */
    @GetMapping("/search")
    public ResponseEntity<Page<BoardDto>> searchBoards(Pageable pageable,
                                                       @RequestParam String keyword) {
        Page<BoardDto> boards = boardService.getBoardsByKeyword(pageable, keyword);
        return ResponseEntity.ok(boards);
    }

    // ==========================================
    // 2. 게시판 생성 및 상태 관리
    // ==========================================

    /** 게시판 생성 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(
            @Valid @RequestBody BoardRequest.Create request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardDto created = boardService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** 게시판 상태 변경 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{boardId}/status")
    public ResponseEntity<BoardDto> updateStatus(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequest.UpdateStatus request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardDto updated = boardService.updateStatus(boardId, request, principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 게시판 삭제 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boardService.delete(boardId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 3. 게시판 정책 및 권한 관리
    // ==========================================

    /** 게시판 정책 수정 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{boardId}/policy")
    public ResponseEntity<BoardPolicyDto> updateBoardPolicy(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequest.UpdatePolicy request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardService.updatePolicy(boardId, request, principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 서브 매니저 추가 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId}/policy/sub-managers")
    public ResponseEntity<BoardPolicyDto> addSubManager(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequest.ManageSubManager request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardService.addSubManager(boardId, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 서브 매니저 해임 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{boardId}/policy/sub-managers")
    public ResponseEntity<BoardPolicyDto> removeSubManager(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequest.ManageSubManager request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardService.removeSubManager(boardId, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 게시판 관리자 권한 위임 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId}/delegate")
    public ResponseEntity<BoardPolicyDto> delegateManager(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardRequest.Delegate request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardService.delegateManager(boardId, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 게시판 관리자 권한 신청 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId}/apply")
    public ResponseEntity<BoardManagerRequestDto> applyForManager(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardManagerRequestDto response = boardService.applyForManager(boardId, principal.getId());
        return ResponseEntity.ok(response);
    }
}