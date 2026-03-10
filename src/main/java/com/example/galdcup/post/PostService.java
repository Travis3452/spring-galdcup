package com.example.galdcup.post;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.post.dto.PostDto;
import com.example.galdcup.post.embedded.Author;
import com.example.galdcup.post.validator.PostValidator;
import com.example.galdcup.postCategory.PostCategory;
import com.example.galdcup.postCategory.validator.PostCategoryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private final BoardValidator boardValidator;
    private final PostValidator postValidator;
    private final PostCategoryValidator postCategoryValidator;

    /**
     * 게시글 조회
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getPosts(Long boardId, Long categoryId, boolean isPopular,
                                  String searchType, String keyword, Pageable pageable) {

        Board board = boardValidator.findByIdOrThrow(boardId);
        Long likeThreshold = isPopular ? board.getBoardPolicy().getLikeThreshold() : null;

        if (keyword != null && !keyword.isBlank()) {
            return getSearch(boardId, categoryId, likeThreshold, searchType, keyword, pageable);
        }

        return getList(boardId, categoryId, likeThreshold, pageable);
    }

    /**
     * 전체 조회
     */
    private Page<PostDto> getList(Long boardId, Long categoryId, Long threshold, Pageable pageable) {
        return postRepository.findPostsFiltered(boardId, categoryId, threshold, pageable)
                .map(PostDto::from);
    }

    /**
     * 검색 조회
     */
    private Page<PostDto> getSearch(Long boardId, Long categoryId, Long threshold,
                                    String searchType, String keyword, Pageable pageable) {

        if ("NICKNAME".equals(searchType)) {
            return postRepository.searchByAuthorNickname(boardId, categoryId, threshold, keyword, pageable)
                    .map(PostDto::from);
        }

        return postRepository.searchByTitleAndContent(boardId, categoryId, threshold, keyword, pageable)
                .map(PostDto::from);
    }

    /**
     * 게시글 생성
     */
    @Transactional
    public PostDto create(Long boardId, Long categoryId, Long authorId, String authorNickname, String title, String content) {
        Board board = boardValidator.getBoardIfOpen(boardId);

        PostCategory category = postCategoryValidator.getIfBelongsToBoard(categoryId, boardId);

        if (category.getType() == PostCategory.CategoryType.NOTICE) {
            boardValidator.getBoardIfManager(boardId, authorId);
        }

        Post post = Post.builder()
                .board(board)
                .postCategory(category)
                .author(new Author(authorId, authorNickname))
                .title(title)
                .content(content)
                .build();

        return PostDto.from(postRepository.save(post));
    }

    /**
     * 게시글 단건 조회 (조회수 증가 포함)
     */
    @Transactional(readOnly = true)
    public Optional<PostDto> findById(Long id) {
        Optional<Post> postOpt = postRepository.findById(id);
        postOpt.ifPresent(post -> incrementViewCount(post.getId()));
        return postOpt.map(PostDto::from);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostDto update(Long postId, Long authorId, String title, String content) {
        Post post = postValidator.findByIdOrThrow(postId);
        postValidator.validateIsAuthor(post, authorId);

        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));

        return PostDto.from(postRepository.save(post));
    }

    /**
     * 게시글 삭제 (본인)
     */
    @Transactional
    public void delete(Long postId, Long authorId) {
        Post post = postValidator.findByIdOrThrow(postId);
        postValidator.validateIsAuthor(post, authorId);

        postRepository.deleteById(postId);
        deleteViewCache(postId);
    }

    /**
     * 게시글 삭제 (관리자)
     */
    @Transactional
    public void deleteForBoardManager(Long postId, Long boardId, Long managerId) {
        postValidator.findByIdOrThrow(postId);
        boardValidator.getBoardIfManager(boardId, managerId);

        postRepository.deleteById(postId);
        deleteViewCache(postId);
    }

    /**
     * 사용자별 작성 게시글 조회
     */
    @Transactional(readOnly = true)
    public Page<PostDto> findByAuthorNickname(String nickname, Pageable pageable) {
        return postRepository.findByAuthorNickname(nickname, pageable).map(PostDto::from);
    }

    private void incrementViewCount(Long postId) {
        try {
            String key = "post:view:" + postId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Redis view increment error: {}", e.getMessage());
        }
    }

    private void deleteViewCache(Long postId) {
        try {
            redisTemplate.delete("post:view:" + postId);
        } catch (Exception e) {
            log.error("Redis view cache delete error: {}", e.getMessage());
        }
    }
}