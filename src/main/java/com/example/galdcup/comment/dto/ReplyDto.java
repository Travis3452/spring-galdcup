package com.example.galdcup.comment.dto;

import com.example.galdcup.comment.Reply;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ReplyDto(
        Long id,
        Long commentId,
        Long authorId,
        String authorNickname,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean isDeleted
) {
    public static ReplyDto from(Reply reply) {
        return ReplyDto.builder()
                .id(reply.getId())
                .commentId(reply.getParentComment().getId())
                .authorId(reply.getAuthor().getId())
                .authorNickname(reply.getAuthor().getNickname())
                .content(reply.getContent())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .isDeleted(reply.isDeleted())
                .build();
    }
}
