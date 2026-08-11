package com.example.galdcup.userAiAgent;

import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.userAiAgent.request.CreateUserAiAgentRequest;
import com.example.galdcup.userAiAgent.response.UserAiAgentDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-agents")
@RequiredArgsConstructor
public class UserAiAgentController implements UserAiAgentApi {

    private final UserAiAgentService userAiAgentService;

    /**
     * AI 용병 생성 (유저당 1대 제한)
     */
    @Override
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<UserAiAgentDto> createAgent(
            @Valid @RequestBody CreateUserAiAgentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        UserAiAgentDto response = userAiAgentService.create(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 AI 용병 정보 조회
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserAiAgentDto> getMyAgent(
            @AuthenticationPrincipal CustomUserDetails principal) {

        return ResponseEntity.ok(userAiAgentService.findMyAgent(principal.getId()));
    }

    /**
     * AI 용병 삭제
     */
    @Override
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAgent(
            @AuthenticationPrincipal CustomUserDetails principal) {

        userAiAgentService.deleteMyAgent(principal.getId());
        return ResponseEntity.noContent().build();
    }
}