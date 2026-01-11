package com.example.galdcup.request.boardmanager.dto;

import com.example.galdcup.request.boardmanager.BoardManagerRequest;

public record BoardManagerRequestDto(
        Long id,
        Long applicantId,
        String applicantEmail,
        String applicantNickname,
        Long boardId,
        String boardTopic,
        BoardManagerRequest.Status status
) {
    public static BoardManagerRequestDto from(BoardManagerRequest boardManagerRequest, String decryptedEmail) {
        return new BoardManagerRequestDto(
                boardManagerRequest.getId(),
                boardManagerRequest.getApplicant().getId(),
                decryptedEmail,
                boardManagerRequest.getApplicant().getNickname(),
                boardManagerRequest.getBoard().getId(),
                boardManagerRequest.getBoard().getTopic(),
                boardManagerRequest.getStatus()
        );
    }
}
