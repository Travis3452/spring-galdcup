package com.example.galdcup.dto.vote;

import com.example.galdcup.entity.Vote;

public record VoteDto(Long id, Long voteSessionId, Long userId, int selectedOptionIndex) {
    public static VoteDto from(Vote vote) {
        return new VoteDto(
                vote.getId(),
                vote.getVoteSession().getId(),
                vote.getUser().getId(),
                vote.getSelectedOptionIndex()
        );
    }
}