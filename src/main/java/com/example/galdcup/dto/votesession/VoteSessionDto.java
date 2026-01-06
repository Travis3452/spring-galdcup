package com.example.galdcup.dto.votesession;

import com.example.galdcup.entity.VoteSession;
import java.time.OffsetDateTime;
import java.util.List;

public record VoteSessionDto(
        Long id,
        Long boardId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        List<String> options,
        List<String> optionImages
) {
    public static VoteSessionDto from(VoteSession voteSession) {
        return new VoteSessionDto(
                voteSession.getId(),
                voteSession.getBoard().getId(),
                voteSession.getStartTime(),
                voteSession.getEndTime(),
                voteSession.getOptions(),
                voteSession.getOptionImages()
        );
    }
}