package com.example.galdcup.board.request;

import com.example.galdcup.board.domain.Board;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BoardRequest {

    public record Create(
            @NotBlank(message = "갈드컵 주제를 입력해야 합니다.")
            @Size(min = 2, max = 50, message = "주제를 2자 이상 50자 이하로 입력하세요.")
            String topic,

            @NotBlank(message = "갈드컵의 설명을 입력해야 합니다.")
            @Size(min = 5, max = 200, message = "설명을 5자 이상 200자 이하로 입력하세요.")
            String description
    ) {}

    public record UpdateStatus(
            @NotNull(message = "상태값은 필수입니다.")
            Board.Status status
    ) {}

    public record UpdatePolicy(
            @Min(value = 1, message = "좋아요 기준은 1 이상이어야 합니다.")
            int likeThreshold
    ) {}

    public record ManageSubManager(
            @NotBlank(message = "닉네임은 필수입니다.")
            String nickname
    ) {}

    /** 관리자 위임 요청 */
    public record Delegate(
            @NotBlank(message = "위임할 유저의 닉네임은 필수입니다.")
            String nickname
    ) {}
}