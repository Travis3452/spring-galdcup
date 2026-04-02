package com.example.galdcup.board.response;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.postCategory.response.PostCategoryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class BoardDetailResponse {
    private BoardDto board;
    private BoardPolicyDto policy;
    private List<PostCategoryDto> categories;

    public static BoardDetailResponse of(Board board) {
        return BoardDetailResponse.builder()
                .board(BoardDto.from(board))
                .policy(BoardPolicyDto.from(board.getBoardPolicy()))
                .categories(board.getPostCategories().stream()
                        .map(PostCategoryDto::from).toList())
                .build();
    }
}