package com.example.galdcup.vote;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.vote.dto.CreateVoteSessionRequest;
import com.example.galdcup.vote.dto.VoteSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards/{boardId}/vote-session")
@RequiredArgsConstructor
public class VoteSessionController {

    private final VoteSessionService voteSessionService;

    /**
     * 투표 세션 생성
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<VoteSessionDto> createVoteSession(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CreateVoteSessionRequest request) {

        VoteSessionDto voteSessionDto = voteSessionService.createVoteSession(
                boardId,
                principal.getId(),
                request
        );

        return ResponseEntity.ok(voteSessionDto);
    }

    /**
     * 투표 세션 조회
     */
    @GetMapping
    public ResponseEntity<VoteSessionDto> getVoteSession(@PathVariable Long boardId) {
        VoteSessionDto voteSessionDto = voteSessionService.getVoteSession(boardId);
        return ResponseEntity.ok(voteSessionDto);
    }
}