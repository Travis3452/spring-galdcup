package com.example.galdcup.board.dto;

import com.example.galdcup.boardPolicy.dto.BoardPolicyDto;
import com.example.galdcup.postCategory.dto.PostCategoryDto;
import com.example.galdcup.voteSession.dto.VoteSessionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDetailResponse {
    private BoardDto board;
    private BoardPolicyDto policy;
    private List<PostCategoryDto> categories;
    private VoteSessionDto activeVoteSession;
}