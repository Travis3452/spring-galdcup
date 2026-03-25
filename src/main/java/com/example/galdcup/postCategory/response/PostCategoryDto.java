package com.example.galdcup.postCategory.response;

import com.example.galdcup.postCategory.domain.PostCategory;
import lombok.Builder;

import java.io.Serializable;

@Builder
public record PostCategoryDto(
        Long id,
        String name,
        String categoryType,
        int sortOrder
) implements Serializable {

    public static PostCategoryDto from(PostCategory postCategory) {
        return PostCategoryDto.builder()
                .id(postCategory.getId())
                .name(postCategory.getName())
                .categoryType(postCategory.getType().name())
                .sortOrder(postCategory.getSortOrder())
                .build();
    }
}