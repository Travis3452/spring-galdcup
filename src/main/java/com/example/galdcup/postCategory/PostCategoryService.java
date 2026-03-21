package com.example.galdcup.postCategory;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.board.event.BoardChangedEvent;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.post.domain.PostRepository;
import com.example.galdcup.postCategory.domain.PostCategory;
import com.example.galdcup.postCategory.domain.PostCategoryRepository;
import com.example.galdcup.postCategory.dto.PostCategoryDto;
import com.example.galdcup.postCategory.dto.PostCategoryRequest;
import com.example.galdcup.postCategory.dto.UpdatePostCategoryRequest;
import com.example.galdcup.postCategory.validator.PostCategoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher eventPublisher;

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

        PostCategory postCategory = PostCategory.createCustom(board, request.name(), maxOrder + 1);

        postCategoryRepository.save(postCategory);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));

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
            postCategoryValidator.validateUniqueNameInBoard(boardId, request.name());
        }

        postCategory.update(request.name(), request.sortOrder());

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));

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
                category.changeSortOrder(req.sortOrder());
            }
        }

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));
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

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));
    }
}