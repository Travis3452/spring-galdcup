package com.example.galdcup.vote;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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