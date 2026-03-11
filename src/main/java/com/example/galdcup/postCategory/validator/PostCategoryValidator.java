package com.example.galdcup.postCategory.validator;

import com.example.galdcup.postCategory.PostCategory;
import com.example.galdcup.postCategory.PostCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostCategoryValidator {

    private final PostCategoryRepository postCategoryRepository;

    /**
     * 카테고리 존재 여부 확인 및 반환
     */
    public PostCategory findByIdOrThrow(Long postCategoryId) {
        return postCategoryRepository.findById(postCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리를 찾을 수 없습니다. (ID: " + postCategoryId + ")"));
    }

    /**
     * 시스템 카테고리(NOTICE, GENERAL)인지 확인 (수정/삭제 방지)
     */
    public void validateRemovable(PostCategory postCategory) {
        if (!postCategory.isRemovable()) {
            throw new IllegalArgumentException("기본 시스템 카테고리(공지사항, 일반)는 변경하거나 삭제할 수 없습니다.");
        }
    }

    /**
     * 게시판 내 카테고리 이름 중복 검사
     */
    public void validateUniqueNameInBoard(Long boardId, String name) {
        if (postCategoryRepository.existsByBoardIdAndName(boardId, name)) {
            throw new IllegalArgumentException("이미 해당 게시판에 '" + name + "' 카테고리가 존재합니다.");
        }
    }

    /**
     * 특정 게시판에 속한 카테고리인지 확인 후 객체 반환 (조회 + 보안 체크 통합)
     */
    public PostCategory getIfBelongsToBoard(Long categoryId, Long boardId) {
        PostCategory postCategory = this.findByIdOrThrow(categoryId);

        if (!postCategory.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시판에 속하지 않은 카테고리입니다.");
        }

        return postCategory;
    }

    /**
     * 병합(이관) 시 통합 검증 로직
     */
    public void validateMigration(PostCategory target, PostCategory destination) {
        if (target.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("자기 자신으로 게시글을 옮길 수 없습니다.");
        }

        if (destination.getType() == PostCategory.CategoryType.NOTICE) {
            throw new IllegalArgumentException("공지사항 카테고리로는 일반 게시글을 옮길 수 없습니다.");
        }

        if (!target.getBoard().getId().equals(destination.getBoard().getId())) {
            throw new IllegalArgumentException("서로 다른 게시판의 카테고리로는 게시글을 병합할 수 없습니다.");
        }
    }
}