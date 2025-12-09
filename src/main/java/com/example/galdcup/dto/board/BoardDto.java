package com.example.galdcup.dto.board;

import com.example.galdcup.entity.Board;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BoardDto(
        Long id,
        Long topicId,
        Board.Status status,
        LocalDateTime createdAt
) {
    public static BoardDto from(Board board) {
        return BoardDto.builder()
                .id(board.getId())
                .topicId(board.getTopic().getId())
                .status(board.getStatus())
                .createdAt(board.getCreatedAt())
                .build();
    }
}