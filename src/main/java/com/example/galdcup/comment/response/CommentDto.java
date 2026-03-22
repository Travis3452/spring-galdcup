package com.example.galdcup.comment.response;

import com.example.galdcup.comment.domain.Comment;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record CommentDto(
        Long id,
        Long boardId,
        Long postId,
        Long authorId,
        String authorNickname,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean isDeleted,
        List<CommentDto> childrenComments
) {
    public static CommentDto from(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .boardId(comment.getPost().getBoard().getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorNickname(comment.getAuthor().getNickname())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isDeleted(comment.isDeleted())
                .childrenComments(comment.getChildrenComments().stream()
                        .map(CommentDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}