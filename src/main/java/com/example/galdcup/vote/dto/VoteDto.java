package com.example.galdcup.vote.dto;

import com.example.galdcup.vote.Vote;

public record VoteDto(Long id, Long voteSessionId, Long userId, int selectedOptionIndex) {
    public static VoteDto from(Vote vote) {
        return new VoteDto(
                vote.getId(),
                vote.getVoteSession().getId(),
                vote.getVoterId(),
                vote.getSelectedOptionIndex()
        );
    }
}