package com.example.galdcup.board.comment;

import com.example.galdcup.board.comment.request.CreateCommentRequest;
import com.example.galdcup.board.comment.request.UpdateCommentRequest;
import com.example.galdcup.board.comment.response.CommentDto;
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

@Tag(name = "Comment", description = "댓글 및 대댓글 관리 API")
public interface CommentApi {

    @Operation(summary = "게시글의 댓글 조회", description = "특정 게시글에 달린 댓글과 대댓글 목록을 페이징하여 조회합니다.")
    ResponseEntity<Page<CommentDto>> getCommentsByPost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            Pageable pageable);

    @Operation(summary = "사용자가 작성한 댓글 조회", description = "특정 사용자가 작성한 모든 댓글 목록을 조회합니다.")
    ResponseEntity<Page<CommentDto>> getCommentsByUser(
            @Parameter(description = "사용자 닉네임") @PathVariable String nickname,
            Pageable pageable);

    @Operation(summary = "댓글 및 대댓글 작성", description = "게시글에 새로운 댓글을 작성하거나, 특정 댓글에 대댓글을 작성합니다.")
    ResponseEntity<CommentDto> createComment(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @RequestBody CreateCommentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "댓글 수정", description = "작성한 댓글의 내용을 수정합니다.")
    ResponseEntity<CommentDto> updateComment(
            @Parameter(description = "댓글 ID") @PathVariable Long id,
            @RequestBody UpdateCommentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    ResponseEntity<CommentDto> deleteComment(
            @Parameter(description = "댓글 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);
}