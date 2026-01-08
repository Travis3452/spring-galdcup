package com.example.galdcup.vote.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateVoteSessionRequest(
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        List<String> options,
        List<String> optionImages
) {}