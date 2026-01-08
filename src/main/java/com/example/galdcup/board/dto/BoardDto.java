package com.example.galdcup.board.dto;

import com.example.galdcup.board.Board;
import lombok.Builder;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Builder
public record BoardDto(
        Long id,
        String topic,
        String description,
        Board.Status status,
        OffsetDateTime createdAt,
        Long adminId,
        String adminNickname
) implements Serializable {
    public static BoardDto from(Board board) {
        return BoardDto.builder()
                .id(board.getId())
                .topic(board.getTopic())
                .description(board.getDescription())
                .status(board.getStatus())
                .createdAt(board.getCreatedAt())
                .adminId(board.getAdmin().getId())
                .adminNickname(board.getAdmin().getNickname())
                .build();
    }
}
