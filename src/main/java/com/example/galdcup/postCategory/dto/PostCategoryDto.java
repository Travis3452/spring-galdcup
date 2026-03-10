package com.example.galdcup.postCategory.dto;

import com.example.galdcup.postCategory.PostCategory;
import lombok.Builder;

import java.io.Serializable;

@Builder
public record PostCategoryDto(Long id, String name, String categoryType) implements Serializable {
    public static PostCategoryDto from(PostCategory postCategory) {
        return PostCategoryDto.builder()
                .id(postCategory.getId())
                .name(postCategory.getName())
                .categoryType(postCategory.getType().name())
                .build();
    }
}
