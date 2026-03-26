package com.example.galdcup.vote;

import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.vote.request.CreateVoteRequest;
import com.example.galdcup.vote.response.VoteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * 투표 생성
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
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