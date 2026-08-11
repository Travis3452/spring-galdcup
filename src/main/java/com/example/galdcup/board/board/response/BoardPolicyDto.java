package com.example.galdcup.board.board.response;

import com.example.galdcup.board.board.domain.BoardPolicy;
import com.example.galdcup.user.response.UserDto;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
public record BoardPolicyDto(
        UserDto boardManager,
        List<UserDto> subManagers,
        long likeThreshold
) implements Serializable {
    public static BoardPolicyDto from(BoardPolicy policy) {
        return BoardPolicyDto.builder()
                .boardManager(policy.getBoardManager() != null
                        ? UserDto.from(policy.getBoardManager())
                        : null)
                .subManagers(policy.getSubManagers().stream()
                        .map(UserDto::from)
                        .toList())
                .likeThreshold(policy.getLikeThreshold())
                .build();
    }
}