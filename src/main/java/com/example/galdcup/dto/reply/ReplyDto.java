package com.example.galdcup.dto.reply;

import com.example.galdcup.entity.Reply;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReplyDto(
        Long id,
        Long commentId,
        String authorNickname,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReplyDto from(Reply reply) {
        return ReplyDto.builder()
                .id(reply.getId())
                .commentId(reply.getParentComment().getId())
                .authorNickname(reply.getCreatedBy().getNickname())
                .content(reply.getContent())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }
}