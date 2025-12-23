package com.example.galdcup.controller;

import com.example.galdcup.dto.vote.CreateVoteRequest;
import com.example.galdcup.dto.vote.VoteDto;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    // 투표하기
    @PostMapping
    public ResponseEntity<VoteDto> createVote(
            @RequestBody CreateVoteRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        VoteDto vote = voteService.createVote(
                request.voteSessionId(),
                principal.getId(),
                request.selectedOptionIndex()
        );

        return ResponseEntity.ok(vote);
    }
}