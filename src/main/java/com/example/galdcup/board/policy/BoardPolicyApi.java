package com.example.galdcup.board.policy;
import com.example.galdcup.board.policy.domain.BoardPolicy;

import com.example.galdcup.board.policy.request.BoardPolicyRequest;
import com.example.galdcup.board.policy.response.BoardManagerRequestDto;
import com.example.galdcup.board.policy.response.BoardPolicyDto;
import com.example.galdcup.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;


@Tag(name = "BoardPolicy", description = "게시판 정책 및 관리자 API")
public interface BoardPolicyApi {

    @Operation(summary = "게시판 정책 수정", description = "게시판 운영 정책을 수정합니다.")
    ResponseEntity<BoardPolicyDto> updateBoardPolicy(
            @PathVariable Long boardId,
            @RequestBody BoardPolicyRequest.UpdatePolicy request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "서브 매니저 추가", description = "게시판 관리를 도울 서브 매니저를 임명합니다.")
    ResponseEntity<BoardPolicyDto> addSubManager(
            @PathVariable Long boardId,
            @RequestBody BoardPolicyRequest.ManageSubManager request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "서브 매니저 해임", description = "임명된 서브 매니저의 권한을 취소합니다.")
    ResponseEntity<BoardPolicyDto> removeSubManager(
            @PathVariable Long boardId,
            @RequestBody BoardPolicyRequest.ManageSubManager request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "관리자 권한 위임", description = "게시판의 소유 관리자 권한을 다른 유저에게 넘깁니다.")
    ResponseEntity<BoardPolicyDto> delegateManager(
            @PathVariable Long boardId,
            @RequestBody BoardPolicyRequest.Delegate request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "관리자 권한 신청", description = "비어있는 게시판의 관리자 권한을 신청합니다.")
    ResponseEntity<BoardManagerRequestDto> applyForManager(
            @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);
}
