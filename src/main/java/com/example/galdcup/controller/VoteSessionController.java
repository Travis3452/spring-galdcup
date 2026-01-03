package com.example.galdcup.controller;

import com.example.galdcup.dto.votesession.CreateVoteSessionRequest;
import com.example.galdcup.dto.votesession.VoteSessionDto;
import com.example.galdcup.entity.VoteSession;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.VoteSessionService;
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<VoteSessionDto> createVoteSession(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CreateVoteSessionRequest request) {

        VoteSession voteSession = voteSessionService.createVoteSession(
                boardId,
                principal.getId(),
                request.startTime(),
                request.endTime(),
                request.options(),
                request.optionImages()
        );

        return ResponseEntity.ok(VoteSessionDto.from(voteSession));
    }

    @GetMapping
    public ResponseEntity<VoteSessionDto> getVoteSession(@PathVariable Long boardId) {
        VoteSession voteSession = voteSessionService.getVoteSession(boardId);
        return ResponseEntity.ok(VoteSessionDto.from(voteSession));
    }
}