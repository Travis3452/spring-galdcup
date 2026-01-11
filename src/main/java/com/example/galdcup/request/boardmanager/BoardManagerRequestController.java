package com.example.galdcup.request.boardmanager;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.request.boardmanager.dto.BoardManagerRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board-manager-requests")
@RequiredArgsConstructor
public class BoardManagerRequestController {

    private final BoardManagerRequestService boardManagerRequestService;

    /** 게시판 관리자 신청 */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{boardId}/apply")
    public ResponseEntity<BoardManagerRequestDto> createBoardManagerRequest(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        BoardManagerRequestDto dto = boardManagerRequestService.createBoardManagerRequest(principal.getId(), boardId);
        return ResponseEntity.ok(dto);
    }

    /** 특정 게시판의 관리자 위임 요청 목록 조회 */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/{boardId}/pending")
    public ResponseEntity<List<BoardManagerRequestDto>> getPendingRequests(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<BoardManagerRequestDto> requests = boardManagerRequestService.getPendingRequests(boardId, principal.getId());
        return ResponseEntity.ok(requests);
    }

    /** 요청 승인 */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<Void> approveRequest(@PathVariable Long requestId) {
        boardManagerRequestService.approveRequest(requestId);
        return ResponseEntity.ok().build();
    }

    /** 요청 거절 */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{requestId}/deny")
    public ResponseEntity<Void> denyRequest(@PathVariable Long requestId) {
        boardManagerRequestService.denyRequest(requestId);
        return ResponseEntity.ok().build();
    }
}