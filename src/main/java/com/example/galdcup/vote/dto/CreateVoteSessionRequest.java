package com.example.galdcup.vote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateVoteSessionRequest(

        @NotNull(message = "투표 시작 시간을 설정해야 합니다.")
        OffsetDateTime startTime,

        @NotNull(message = "투표 종료 시간을 설정해야 합니다.")
        OffsetDateTime endTime,

        @Size(min = 2, max = 10, message = "투표 선택지를 2개 이상 10개 이하로 설정해야 합니다.")
        List<@NotBlank(message = "선택지는 비어 있을 수 없습니다.") String> options,

        @Size(min = 2, max = 10, message = "투표 선택지의 소개 이미지를 2개 이상 10개 이하로 설정해야 합니다.")
        List<String> optionImages
) {}