package com.example.galdcup.comment.dto;

import com.example.galdcup.comment.Comment;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record CommentDto(
        Long id,
        Long postId,
        Long authorId,
        String authorNickname,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CommentDto from(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorNickname(comment.getAuthor().getNickname())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}