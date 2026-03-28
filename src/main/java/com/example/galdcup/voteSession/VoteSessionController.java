package com.example.galdcup.voteSession;

import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.gemini.GeminiService;
import com.example.galdcup.gemini.response.GeminiResponse;
import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import com.example.galdcup.opinionAnalysis.OpinionAnalysisService;
import com.example.galdcup.vote.VoteService;
import com.example.galdcup.vote.request.CreateVoteRequest;
import com.example.galdcup.vote.response.VoteDto;
import com.example.galdcup.voteSession.request.CreateVoteSessionRequest;
import com.example.galdcup.voteSession.response.VoteSessionDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/boards/{boardId}/vote-session")
@RequiredArgsConstructor
public class VoteSessionController {

    private final VoteSessionService voteSessionService;
    private final VoteService voteService;
    private final GeminiService geminiService;
    private final OpinionAnalysisService opinionAnalysisService;

    /**
     * 투표 세션 생성
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<VoteSessionDto> createVoteSession(@PathVariable Long boardId,
                                                            @AuthenticationPrincipal CustomUserDetails principal,
                                                            @Valid @RequestBody CreateVoteSessionRequest request) {
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
    public ResponseEntity<Optional<VoteSessionDto>> getVoteSession(@PathVariable Long boardId) {
        Optional<VoteSessionDto> voteSessionDto = voteSessionService.getActiveVoteSession(boardId);
        return ResponseEntity.ok(voteSessionDto);
    }

    /**
     * 과거 투표 세션 조회
     */
    @GetMapping("/history")
    public ResponseEntity<Page<VoteSessionDto>> getPastVoteSessions(@PathVariable Long boardId,
                                                                    Pageable pageable) {
        Page<VoteSessionDto> voteSessionDtoPage = voteSessionService.getPastVoteSessions(boardId, pageable);
        return ResponseEntity.ok(voteSessionDtoPage);
    }

    /**
     * 투표 세션 즉시 마감
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{voteSessionId}/finish")
    public ResponseEntity<Void> finishVoteSession(@PathVariable Long boardId,
                                                  @PathVariable Long voteSessionId,
                                                  @AuthenticationPrincipal CustomUserDetails principal) {
        voteSessionService.finishVoteSession(boardId, voteSessionId, principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * [AI 추천] 게시판 성격에 맞는 갈드컵 주제 및 선택지 추천
     */
    @RateLimit(type = RateLimitType.EXTERNAL)
    @GetMapping("/recommend")
    public ResponseEntity<GeminiResponse> recommendVoteSession(@PathVariable Long boardId) {
        GeminiResponse recommendation = geminiService.getRecommendation(boardId);

        return ResponseEntity.ok(recommendation);
    }

    @GetMapping("/{voteSessionId}/opinion-analysis")
    public ResponseEntity<OpinionAnalysisResponse> getOpinionAnalysis(@PathVariable Long voteSessionId) {
        OpinionAnalysisResponse cachedAnalysis = opinionAnalysisService.getCachedAnalysis(voteSessionId);

        if (cachedAnalysis == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(cachedAnalysis);
    }

    /**
     * [AI 분석] 현재 수집된 댓글을 바탕으로 실시간 여론(지지율) 분석
     */
    @RateLimit(type = RateLimitType.EXTERNAL)
    @PostMapping("/{voteSessionId}/opinion-analysis")
    public ResponseEntity<OpinionAnalysisResponse> opinionAnalysis(@PathVariable Long voteSessionId) {
        OpinionAnalysisResponse newAnalysis = opinionAnalysisService.performAnalysisAndCache(voteSessionId);
        return ResponseEntity.ok(newAnalysis);
    }

    /**
     * 투표하기
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{voteSessionId}/votes")
    public ResponseEntity<VoteDto> createVote(@PathVariable Long boardId,
                                              @PathVariable Long voteSessionId,
                                              @RequestBody CreateVoteRequest request,
                                              @AuthenticationPrincipal CustomUserDetails principal) {

        VoteDto vote = voteService.createVote(
                voteSessionId,
                principal.getId(),
                request.selectedOptionIndex()
        );

        return ResponseEntity.ok(vote);
    }
}