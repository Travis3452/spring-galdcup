package com.example.galdcup.board.voteSession;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import com.example.galdcup.gemini.response.VoteSessionContextResponse;
import com.example.galdcup.board.vote.request.CreateVoteRequest;
import com.example.galdcup.board.vote.response.VoteDto;
import com.example.galdcup.board.voteSession.request.CreateVoteSessionRequest;
import com.example.galdcup.board.voteSession.response.VoteSessionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;

@Tag(name = "Vote Session", description = "갈드컵 투표 세션 생성, AI 추천 및 실시간 분석 API")
public interface VoteSessionApi {

    @Operation(summary = "투표 세션 생성", description = "게시판 내에 새로운 투표 주제와 선택지들을 생성합니다.")
    ResponseEntity<VoteSessionDto> createVoteSession(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CreateVoteSessionRequest request);

    @Operation(summary = "최신 투표 세션 조회", description = "해당 게시판에서 현재 진행 중인 가장 최근의 투표 세션을 조회합니다.")
    ResponseEntity<Optional<VoteSessionDto>> getLatestVoteSession(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId);

    @Operation(summary = "과거 투표 세션 조회", description = "종료된 지난 투표 세션 목록을 페이징 조회합니다.")
    ResponseEntity<Page<VoteSessionDto>> getPastVoteSessions(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            Pageable pageable);

    @Operation(summary = "투표 세션 즉시 마감", description = "진행 중인 투표를 즉시 종료합니다.")
    ResponseEntity<Void> finishVoteSession(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "투표 세션 ID") @PathVariable Long voteSessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "[AI 추천] 갈드컵 주제 추천", description = "Gemini AI가 게시판의 성격과 트렌드를 분석하여 흥미로운 투표 주제와 선택지를 추천합니다.")
    ResponseEntity<VoteSessionContextResponse> recommendVoteSession(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId);

    @Operation(summary = "[AI 분석] 저장된 여론 데이터 조회", description = "이미 분석되어 캐싱된 실시간 여론(지지율 등) 데이터를 가져옵니다.")
    ResponseEntity<OpinionAnalysisResponse> getOpinionAnalysis(
            @Parameter(description = "투표 세션 ID") @PathVariable Long voteSessionId);

    @Operation(summary = "[AI 분석] 실시간 여론 분석 실행", description = "수집된 댓글과 투표 데이터를 바탕으로 Gemini AI가 현재 여론 지지율을 새로 분석합니다.")
    ResponseEntity<OpinionAnalysisResponse> opinionAnalysis(
            @Parameter(description = "투표 세션 ID") @PathVariable Long voteSessionId);

    @Operation(summary = "투표하기", description = "특정 투표 세션의 선택지 중 하나에 투표합니다.")
    ResponseEntity<VoteDto> createVote(
            @Parameter(description = "게시판 ID") @PathVariable Long boardId,
            @Parameter(description = "투표 세션 ID") @PathVariable Long voteSessionId,
            @RequestBody CreateVoteRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);
}