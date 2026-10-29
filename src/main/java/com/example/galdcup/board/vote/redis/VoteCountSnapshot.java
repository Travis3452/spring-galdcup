package com.example.galdcup.board.vote.redis;

import java.util.Map;

/** Redis에서 원자적으로 읽은 집계와 변경 번호, 종료 요청 시각 */
public record VoteCountSnapshot(
        Map<String, String> counts,
        String revision,
        Long closedAtEpochMilli) {

    public VoteCountSnapshot {
        counts = Map.copyOf(counts);
    }
}
