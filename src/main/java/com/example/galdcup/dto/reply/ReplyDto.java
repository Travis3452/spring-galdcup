package com.example.galdcup.dto.reply;

import com.example.galdcup.entity.Reply;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Builder
public record ReplyDto(
        Long id,
        Long commentId,
        Long authorId,
        String authorNickname,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ReplyDto from(Reply reply) {
        return ReplyDto.builder()
                .id(reply.getId())
                .commentId(reply.getParentComment().getId())
                .authorId(reply.getCreatedBy().getId())
                .authorNickname(reply.getCreatedBy().getNickname())
                .content(reply.getContent())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }
}