package com.example.galdcup.voteSession.dto;

import com.example.galdcup.vote.dto.VoteOptionDto;
import com.example.galdcup.voteSession.domain.VoteSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteSessionDto implements Serializable {

    private Long id;
    private Long boardId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private List<VoteOptionDto> options;

    public static VoteSessionDto from(VoteSession voteSession) {
        return VoteSessionDto.builder()
                .id(voteSession.getId())
                .boardId(voteSession.getBoard().getId())
                .startTime(voteSession.getStartTime())
                .endTime(voteSession.getEndTime())
                .options(voteSession.getOptions().stream()
                        .map(VoteOptionDto::from)
                        .toList())
                .build();
    }
}