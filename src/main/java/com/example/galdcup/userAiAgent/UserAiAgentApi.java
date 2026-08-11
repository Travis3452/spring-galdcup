package com.example.galdcup.userAiAgent;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.userAiAgent.request.CreateUserAiAgentRequest;
import com.example.galdcup.userAiAgent.response.UserAiAgentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "UserAiAgent", description = "AI 용병 생성 및 관리 API")
public interface UserAiAgentApi {

    @Operation(summary = "AI 용병 생성", description = "로그인한 유저의 맞춤형 AI 용병을 24시간 시한부로 생성합니다.")
    ResponseEntity<UserAiAgentDto> createAgent(
            @RequestBody CreateUserAiAgentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "내 AI 용병 조회", description = "현재 활성화되어 있는 본인의 AI 용병 정보를 조회합니다.")
    ResponseEntity<UserAiAgentDto> getMyAgent(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "AI 용병 삭제", description = "생성되어 있는 본인의 AI 용병을 즉시 삭제합니다.")
    ResponseEntity<Void> deleteAgent(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);
}