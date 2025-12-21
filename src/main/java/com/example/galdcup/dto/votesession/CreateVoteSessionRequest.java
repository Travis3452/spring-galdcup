package com.example.galdcup.dto.votesession;

import java.time.LocalDateTime;
import java.util.List;

public record CreateVoteSessionRequest(
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<String> options
) {}
