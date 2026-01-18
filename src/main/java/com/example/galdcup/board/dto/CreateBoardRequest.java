package com.example.galdcup.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(

        @NotBlank(message = "갈드컵 주제를 입력해야 합니다.")
        @Size(min = 2, max = 50, message = "주제를 2자 이상 50자 이하로 입력하세요.")
        String topic,

        @NotBlank(message = "갈드컵의 설명을 입력해야 합니다.")
        @Size(min = 5, max = 200, message = "설명을 5자 이상 200자 이하로 입력하세요.")
        String description
) {}
