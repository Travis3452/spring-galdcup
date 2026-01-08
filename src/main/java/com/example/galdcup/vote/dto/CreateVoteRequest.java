package com.example.galdcup.vote.dto;

public record CreateVoteRequest(Long voteSessionId, int selectedOptionIndex) {
}