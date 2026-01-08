package com.example.galdcup.post.dto;

import com.example.galdcup.post.Post;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record PostDto(
        Long id,
        Long boardId,
        Long authorId,
        String authorNickname,
        Long view,
        String title,
        String content,
        Long likeCount,
        Long dislikeCount,
        OffsetDateTime createdAt
) {
    public static PostDto from(Post post) {
        return PostDto.builder()
                .id(post.getId())
                .boardId(post.getBoard().getId())
                .authorId(post.getAuthor().getId())
                .authorNickname(post.getAuthor().getNickname())
                .view(post.getView())
                .title(post.getTitle())
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}