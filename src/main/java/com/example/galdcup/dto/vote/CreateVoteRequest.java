package com.example.galdcup.dto.vote;

public record CreateVoteRequest(Long voteSessionId, int selectedOptionIndex) {
}