package com.example.galdcup.vote.response;

import com.example.galdcup.vote.domain.Vote;

public record VoteDto(String id, Long voteSessionId, Long userId, int selectedOptionIndex) {
    public static VoteDto from(Vote vote) {
        return new VoteDto(
                vote.getId(),
                vote.getVoteSessionId(),
                vote.getUserId(),
                vote.getSelectedOptionIndex()
        );
    }
}