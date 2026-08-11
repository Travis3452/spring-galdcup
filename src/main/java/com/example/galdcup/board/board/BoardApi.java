package com.example.galdcup.board.board;

import com.example.galdcup.board.board.request.BoardRequest;
import com.example.galdcup.board.board.response.BoardDetailResponse;
import com.example.galdcup.board.board.response.BoardDto;
import com.example.galdcup.board.board.response.BoardManagerRequestDto;
import com.example.galdcup.board.board.response.BoardPolicyDto;
import com.example.galdcup.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Board", description = "게시판 조회 및 관리 API")
public interface BoardApi {

    @Operation(summary = "전체 게시판 목록 조회", description = "페이징을 지원하는 전체 게시판 목록입니다.")
    ResponseEntity<Page<BoardDto>> getBoards(Pageable pageable);

    @Operation(summary = "인기 게시판 목록 조회", description = "조회수 기반 상위 게시판 목록을 반환합니다.")
    ResponseEntity<List<BoardDto>> getPopularBoards();

    @Operation(summary = "특정 게시판 정보 조회", description = "ID를 통해 단일 게시판의 기본 정보를 확인합니다.")
    ResponseEntity<BoardDto> getBoard(@PathVariable Long boardId);

    @Operation(summary = "게시판 상세 데이터 통합 조회", description = "정책, 카테고리 등 게시판 진입 시 필요한 모든 데이터를 조회합니다.")
    ResponseEntity<BoardDetailResponse> getBoardDetails(@PathVariable Long boardId);

    @Operation(summary = "게시판 검색", description = "키워드를 포함하는 게시판 목록을 검색합니다.")
    ResponseEntity<Page<BoardDto>> searchBoards(Pageable pageable, @RequestParam String keyword);

    @Operation(summary = "게시판 생성", description = "관리자 및 매니저 권한으로 새로운 게시판을 생성합니다.")
    ResponseEntity<BoardDto> createBoard(
            @RequestBody BoardRequest.Create request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "게시판 상태 변경", description = "게시판의 활성화/비활성화 상태를 변경합니다.")
    ResponseEntity<BoardDto> updateStatus(
            @PathVariable Long boardId,
            @RequestBody BoardRequest.UpdateStatus request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "게시판 삭제", description = "게시판을 영구적으로 삭제합니다.")
    ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "게시판 정책 수정", description = "게시판 운영 정책을 수정합니다.")
    ResponseEntity<BoardPolicyDto> updateBoardPolicy(
            @PathVariable Long boardId,
            @RequestBody BoardRequest.UpdatePolicy request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "서브 매니저 추가", description = "게시판 관리를 도울 서브 매니저를 임명합니다.")
    ResponseEntity<BoardPolicyDto> addSubManager(
            @PathVariable Long boardId,
            @RequestBody BoardRequest.ManageSubManager request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "서브 매니저 해임", description = "임명된 서브 매니저의 권한을 취소합니다.")
    ResponseEntity<BoardPolicyDto> removeSubManager(
            @PathVariable Long boardId,
            @RequestBody BoardRequest.ManageSubManager request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "관리자 권한 위임", description = "게시판의 소유 관리자 권한을 다른 유저에게 넘깁니다.")
    ResponseEntity<BoardPolicyDto> delegateManager(
            @PathVariable Long boardId,
            @RequestBody BoardRequest.Delegate request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "관리자 권한 신청", description = "비어있는 게시판의 관리자 권한을 신청합니다.")
    ResponseEntity<BoardManagerRequestDto> applyForManager(
            @PathVariable Long boardId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);
}
