package com.example.galdcup.board.vote.response;

import com.example.galdcup.board.vote.domain.VoteOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteOptionDto {

    private String label;
    private String imageUrl;
    private Long count;

    public static VoteOptionDto from(VoteOption option) {
        return VoteOptionDto.builder()
                .label(option.getLabel())
                .imageUrl(option.getImageUrl())
                .count(option.getCount())
                .build();
    }
}