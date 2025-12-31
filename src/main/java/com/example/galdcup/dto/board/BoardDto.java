package com.example.galdcup.dto.board;

import com.example.galdcup.entity.Board;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
public record BoardDto(
        Long id,
        String topic,
        String description,
        Board.Status status,
        LocalDateTime createdAt
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