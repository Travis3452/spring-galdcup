package com.example.galdcup.board.postCategory.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePostCategoryRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long id,

        @Size(min = 2, max = 10, message = "카테고리 이름은 2자 이상 10자 이하로 입력해야 합니다.")
        String name,

        Integer sortOrder
) {
}