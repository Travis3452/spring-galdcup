package com.example.galdcup.dto.post;

import com.example.galdcup.entity.Post;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostDto(
        Long id,
        Long boardId,
        String authorNickname,
        String title,
        String content,
        LocalDateTime createdAt
) {
    public static PostDto from(Post post) {
        return PostDto.builder()
                .id(post.getId())
                .boardId(post.getBoard().getId())
                .authorNickname(post.getAuthor().getNickname())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .build();
    }
}