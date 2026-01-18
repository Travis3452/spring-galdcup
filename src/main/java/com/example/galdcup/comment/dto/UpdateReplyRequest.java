package com.example.galdcup.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReplyRequest(
        @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.")
        @Size(min = 1, max = 300, message = "댓글 내용은 1자 이상 300자 이하로 입력하세요.")
        String content
) {}