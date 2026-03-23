package com.example.galdcup.voteSession.request;

import com.example.galdcup.vote.request.VoteOptionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateVoteSessionRequest(
        @NotBlank(message = "투표 주제를 입력해야 합니다.")
        @Size(max = 50, message = "투표 주제는 50자를 초과할 수 없습니다.")
        String topic,

        @Size(max = 255, message = "투표 설명은 255자를 초과할 수 없습니다.")
        String description,

        @NotNull(message = "투표 시작 시간을 설정해야 합니다.")
        OffsetDateTime startTime,

        @NotNull(message = "투표 종료 시간을 설정해야 합니다.")
        OffsetDateTime endTime,

        @NotNull(message = "투표 선택지를 추가해야 합니다.")
        @Size(min = 2, max = 10, message = "투표 선택지는 2개에서 10개 사이여야 합니다.")
        @Valid
        List<VoteOptionRequest> options
) {}