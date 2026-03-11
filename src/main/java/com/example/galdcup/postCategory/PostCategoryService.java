package com.example.galdcup.postCategory;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.post.PostRepository;
import com.example.galdcup.postCategory.dto.PostCategoryRequest;
import com.example.galdcup.postCategory.dto.PostCategoryDto;
import com.example.galdcup.postCategory.validator.PostCategoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCategoryService {

    private final PostCategoryRepository postCategoryRepository;
    private final PostRepository postRepository;

    private final PostCategoryValidator postCategoryValidator;
    private final BoardValidator boardValidator;

    public List<PostCategoryDto> findByBoardId(Long boardId) {
        boardValidator.findByIdOrThrow(boardId);

        return postCategoryRepository.findByBoardId(boardId).stream()
                .map(PostCategoryDto::from)
                .toList();
    }

    @Transactional
    public PostCategoryDto createCustomPostCategory(PostCategoryRequest request, Long boardId, Long boardManagerId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, boardManagerId);
        postCategoryValidator.validateUniqueNameInBoard(boardId, request.name());

        PostCategory postCategory = PostCategory.builder()
                .name(request.name())
                .type(PostCategory.CategoryType.CUSTOM)
                .board(board)
                .build();

        board.addPostCategory(postCategory);

        return PostCategoryDto.from(postCategory);
    }

    @Transactional
    public PostCategoryDto updateCustomPostCategory(PostCategoryRequest request, Long boardId, Long postCategoryId, Long boardManagerId) {
        boardValidator.getBoardIfBoardManager(boardId, boardManagerId);

        PostCategory postCategory = postCategoryValidator.getIfBelongsToBoard(postCategoryId, boardId);
        postCategoryValidator.validateRemovable(postCategory);

        if (!postCategory.getName().equals(request.name())) {
            postCategoryValidator.validateUniqueNameInBoard(boardId, request.name());
            postCategory.setName(request.name());
        }

        return PostCategoryDto.from(postCategory);
    }

    @Transactional
    public void deleteAndMigrate(Long boardId, Long categoryId, Long moveToId, Long boardManagerId) {
        boardValidator.getBoardIfBoardManager(boardId, boardManagerId);

        PostCategory target = postCategoryValidator.getIfBelongsToBoard(categoryId, boardId);
        PostCategory destination = postCategoryValidator.getIfBelongsToBoard(moveToId, boardId);

        postCategoryValidator.validateMigration(target, destination);

        postRepository.updateCategoryBulk(target, destination);
        postCategoryRepository.delete(target);
    }
}