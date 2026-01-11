package com.example.galdcup.vote.dto;

import com.example.galdcup.vote.VoteOption;

public record VoteOptionDto(
        String label,
        String imageUrl,
        Long count
) {
    public static VoteOptionDto from(VoteOption option) {
        return new VoteOptionDto(
                option.getLabel(),
                option.getImageUrl(),
                option.getCount()
        );
    }
}