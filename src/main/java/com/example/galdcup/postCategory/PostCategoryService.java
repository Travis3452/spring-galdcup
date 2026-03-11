package com.example.galdcup.postCategory;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.post.PostRepository;
import com.example.galdcup.postCategory.dto.PostCategoryDto;
import com.example.galdcup.postCategory.dto.PostCategoryRequest;
import com.example.galdcup.postCategory.dto.UpdatePostCategoryRequest;
import com.example.galdcup.postCategory.validator.PostCategoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCategoryService {

    private final PostCategoryRepository postCategoryRepository;
    private final PostRepository postRepository;

    private final PostCategoryValidator postCategoryValidator;
    private final BoardValidator boardValidator;

    /**
     * 게시판별 카테고리 조회
     */
    public List<PostCategoryDto> findByBoardId(Long boardId) {
        boardValidator.findByIdOrThrow(boardId);

        return postCategoryRepository.findByBoardIdOrderBySortOrderAsc(boardId).stream()
                .map(PostCategoryDto::from)
                .toList();
    }

    /**
     * 새 카테고리 생성
     */
    @Transactional
    public PostCategoryDto createCustomPostCategory(PostCategoryRequest request, Long boardId, Long boardManagerId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, boardManagerId);
        postCategoryValidator.validateUniqueNameInBoard(boardId, request.name());

        int maxOrder = postCategoryRepository.findMaxSortOrderByBoardId(boardId).orElse(0);

        PostCategory postCategory = PostCategory.builder()
                .name(request.name())
                .type(PostCategory.CategoryType.CUSTOM)
                .sortOrder(maxOrder + 1)
                .board(board)
                .build();

        postCategoryRepository.save(postCategory);
        return PostCategoryDto.from(postCategory);
    }

    /**
     * 카테고리 단일 수정 (이름 및 순서)
     */
    @Transactional
    public PostCategoryDto updateCategory(Long boardId, UpdatePostCategoryRequest request, Long boardManagerId) {
        boardValidator.getBoardIfBoardManager(boardId, boardManagerId);

        PostCategory postCategory = postCategoryValidator.getIfBelongsToBoard(request.id(), boardId);

        if (request.name() != null && !postCategory.getName().equals(request.name())) {
            postCategoryValidator.validateRemovable(postCategory);
            postCategoryValidator.validateUniqueNameInBoard(boardId, request.name());
            postCategory.setName(request.name());
        }

        if (request.sortOrder() != null) {
            postCategory.setSortOrder(request.sortOrder());
        }

        return PostCategoryDto.from(postCategory);
    }

    /**
     * 카테고리 순서 일괄 변경
     */
    @Transactional
    public void updateCategoryBatch(Long boardId, List<UpdatePostCategoryRequest> requests, Long boardManagerId) {
        boardValidator.getBoardIfBoardManager(boardId, boardManagerId);

        for (UpdatePostCategoryRequest req : requests) {
            PostCategory category = postCategoryValidator.getIfBelongsToBoard(req.id(), boardId);

            if (req.sortOrder() != null) {
                category.setSortOrder(req.sortOrder());
            }
        }
    }

    /**
     * 카테고리 삭제 및 게시글 이관
     */
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