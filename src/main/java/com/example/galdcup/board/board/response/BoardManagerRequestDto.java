package com.example.galdcup.board.board.response;

import com.example.galdcup.board.board.domain.BoardManagerRequest;

public record BoardManagerRequestDto(
        Long id,
        Long applicantId,
        String applicantEmail,
        String applicantNickname,
        Long boardId,
        String boardTopic,
        BoardManagerRequest.Status status
) {
    public static BoardManagerRequestDto from(BoardManagerRequest boardManagerRequest) {
        return new BoardManagerRequestDto(
                boardManagerRequest.getId(),
                boardManagerRequest.getApplicant().getId(),
                boardManagerRequest.getApplicant().getEmail(),
                boardManagerRequest.getApplicant().getNickname(),
                boardManagerRequest.getBoard().getId(),
                boardManagerRequest.getBoard().getTopic(),
                boardManagerRequest.getStatus()
        );
    }
}
