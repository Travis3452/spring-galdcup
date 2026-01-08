package com.example.galdcup.vote.dto;

import com.example.galdcup.vote.embedded.VoteOption;

public record VoteOptionDto(
        String label,
        String imageUrl
) {
    public static VoteOptionDto from(VoteOption option) {
        return new VoteOptionDto(
                option.getLabel(),
                option.getImageUrl()
        );
    }
}