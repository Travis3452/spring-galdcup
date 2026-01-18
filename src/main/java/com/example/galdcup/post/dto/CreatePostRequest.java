package com.example.galdcup.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull
        Long boardId,

        @NotBlank(message = "제목은 비어 있을 수 없습니다.")
        @Size(min = 1, max = 50,  message = "게시글 제목을 1자 이상 50자 이하로 입력하세요.")
        String title,

        @NotBlank(message = "내용은 비어 있을 수 없습니다.")
        @Size(min = 1, max = 2000, message = "게시글 내용을 1자 이상 2000자 이하로 입력하세요.")
        String content
) {}