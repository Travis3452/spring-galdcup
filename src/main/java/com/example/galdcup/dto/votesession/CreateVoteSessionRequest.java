package com.example.galdcup.dto.votesession;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateVoteSessionRequest(
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        List<String> options,
        List<String> optionImages
) {}