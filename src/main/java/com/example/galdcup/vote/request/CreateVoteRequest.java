package com.example.galdcup.vote.request;

public record CreateVoteRequest(Long voteSessionId, int selectedOptionIndex) {
}