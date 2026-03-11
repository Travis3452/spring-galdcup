package com.example.galdcup.board.dto;

import com.example.galdcup.boardPolicy.dto.BoardPolicyDto;
import com.example.galdcup.postCategory.dto.PostCategoryDto;
import com.example.galdcup.voteSession.dto.VoteSessionDto;
import lombok.Builder;

import java.util.List;

@Builder
public record BoardDetailResponse(
        BoardDto board,
        BoardPolicyDto policy,
        List<PostCategoryDto> categories,
        VoteSessionDto activeVoteSession
) {}