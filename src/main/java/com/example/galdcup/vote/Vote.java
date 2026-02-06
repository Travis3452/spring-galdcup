package com.example.galdcup.vote;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;


@RedisHash(value = "vote")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class Vote {

    @Id
    private String id;

    private Long userId;
    private Long voteSessionId;
    private int selectedOptionIndex;

    @TimeToLive
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