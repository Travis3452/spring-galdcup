package com.example.galdcup.board.policy;

import com.example.galdcup.board.policy.request.BoardPolicyRequest;
import com.example.galdcup.board.policy.response.BoardManagerRequestDto;
import com.example.galdcup.board.policy.response.BoardPolicyDto;
import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardPolicyController implements BoardPolicyApi {
    private final BoardPolicyService boardPolicyService;

    // ==========================================
    // 3. 게시판 정책 및 권한 관리
    // ==========================================

    /** 게시판 정책 수정 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{boardId}/policy")
    public ResponseEntity<BoardPolicyDto> updateBoardPolicy(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardPolicyRequest.UpdatePolicy request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardPolicyService.updatePolicy(boardId, request, principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 서브 매니저 추가 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId}/policy/sub-managers")
    public ResponseEntity<BoardPolicyDto> addSubManager(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardPolicyRequest.ManageSubManager request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardPolicyService.addSubManager(boardId, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 서브 매니저 해임 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{boardId}/policy/sub-managers")
    public ResponseEntity<BoardPolicyDto> removeSubManager(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardPolicyRequest.ManageSubManager request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardPolicyService.removeSubManager(boardId, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 게시판 관리자 권한 위임 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId}/delegate")
    public ResponseEntity<BoardPolicyDto> delegateManager(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardPolicyRequest.Delegate request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardPolicyDto updated = boardPolicyService.delegateManager(boardId, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 게시판 관리자 권한 신청 */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId}/apply")
    public ResponseEntity<BoardManagerRequestDto> applyForManager(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        BoardManagerRequestDto response = boardPolicyService.applyForManager(boardId, principal.getId());
        return ResponseEntity.ok(response);
    }
}
