package com.example.galdcup.vote.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vote {
    private String id;

    private Long userId;
    private Long voteSessionId;
    private int selectedOptionIndex;
    private Long ttl;

    public static Vote of(Long voteSessionId, Long userId, int selectedOptionIndex, Long ttl) {
        return Vote.builder()
                .id(voteSessionId + ":" + userId)
                .userId(userId)
                .voteSessionId(voteSessionId)
                .selectedOptionIndex(selectedOptionIndex)
                .ttl(ttl)
                .build();
    }
}