package com.example.galdcup.board.response;

import com.example.galdcup.board.domain.Board;
import lombok.Builder;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Builder
public record BoardDto(
        Long id,
        String topic,
        String description,
        Board.Status status,
        OffsetDateTime createdAt
) implements Serializable {
    public static BoardDto from(Board board) {
        return BoardDto.builder()
                .id(board.getId())
                .topic(board.getTopic())
                .description(board.getDescription())
                .status(board.getStatus())
                .createdAt(board.getCreatedAt())
                .build();
    }
}