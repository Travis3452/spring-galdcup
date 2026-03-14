package com.example.galdcup.post.dto;

import com.example.galdcup.post.Post;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record PostDto(
        Long id,
        Long boardId,
        Long categoryId,
        String categoryName,
        String categoryType,
        Long authorId,
        String authorNickname,
        Long viewCount,
        String title,
        String content,
        Long likeCount,
        Long dislikeCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PostDto from(Post post) {
        return PostDto.builder()
                .id(post.getId())
                .boardId(post.getBoard().getId())
                .categoryId(post.getPostCategory().getId())
                .categoryName(post.getPostCategory().getName())
                .categoryType(post.getPostCategory().getType().name())
                .authorId(post.getAuthor().getId())
                .authorNickname(post.getAuthor().getNickname())
                .viewCount(post.getViewCount())
                .title(post.getTitle())
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}