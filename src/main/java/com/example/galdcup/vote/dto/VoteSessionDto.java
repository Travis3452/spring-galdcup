package com.example.galdcup.vote.dto;

import com.example.galdcup.vote.VoteSession;

import java.time.OffsetDateTime;
import java.util.List;

public record VoteSessionDto(
        Long id,
        Long boardId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        List<VoteOptionDto> options
) {
    public static VoteSessionDto from(VoteSession voteSession) {
        return new VoteSessionDto(
                voteSession.getId(),
                voteSession.getBoard().getId(),
                voteSession.getStartTime(),
                voteSession.getEndTime(),
                voteSession.getOptions().stream()
                        .map(VoteOptionDto::from)
                        .toList()
        );
    }
}