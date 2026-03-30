package com.example.galdcup.voteSession.response;

import com.example.galdcup.vote.domain.VoteOption;
import com.example.galdcup.vote.response.VoteOptionDto;
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
    private String topic;
    private String description;
    private Long totalVotes;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private List<VoteOptionDto> options;

    public boolean isActive() {
        OffsetDateTime now = OffsetDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    public static VoteSessionDto from(VoteSession voteSession) {
            boolean isActive = voteSession.isActive();

        long calculatedTotalVotes = voteSession.getOptions().stream()
                .mapToLong(VoteOption::getCount)
                .sum();

        return VoteSessionDto.builder()
                .id(voteSession.getId())
                .boardId(voteSession.getBoard().getId())
                .topic(voteSession.getTopic())
                .description(voteSession.getDescription())
                .startTime(voteSession.getStartTime())
                .endTime(voteSession.getEndTime())
                .totalVotes(calculatedTotalVotes)
                .options(voteSession.getOptions().stream()
                        .map(option -> {
                            VoteOptionDto optionDto = VoteOptionDto.from(option);
                            if (isActive) {
                                optionDto.setCount(null);
                            }
                            return optionDto;
                        })
                        .toList())
                .build();
    }
}