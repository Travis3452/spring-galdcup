package com.example.galdcup.board.vote.request;

public record CreateVoteRequest(Long voteSessionId, int selectedOptionIndex) {
}