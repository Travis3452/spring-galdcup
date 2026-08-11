package com.example.galdcup.board.postCategory.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCategoryRequest(
        @NotNull(message = "게시글 카테고리 이름을 입력하세요.")
        @Size(min = 2, max = 10, message = "카테고리 이름은 2글자 이상 10글자 이하로 정해야합니다.")
        String name
) {
}
