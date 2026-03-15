package com.example.galdcup.post;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.common.CachedPageResponse;
import com.example.galdcup.post.dto.PostDto;
import com.example.galdcup.post.embedded.Author;
import com.example.galdcup.post.event.PostChangedEvent;
import com.example.galdcup.post.validator.PostValidator;
import com.example.galdcup.postCategory.PostCategory;
import com.example.galdcup.postCategory.validator.PostCategoryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.PolicyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostRedisManager postRedisManager;
    private final ApplicationEventPublisher eventPublisher;

    private final BoardValidator boardValidator;
    private final PostValidator postValidator;
    private final PostCategoryValidator postCategoryValidator;

    private final PolicyFactory htmlSanitizer;

    /**
     * 게시글 목록 조회
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
     * 게시글 생성
     */
    @Transactional
    public PostDto create(Long boardId, Long categoryId, Long authorId, String authorNickname, String title, String content) {
        Board board = boardValidator.getBoardIfOpen(boardId);
        PostCategory category = postCategoryValidator.getIfBelongsToBoard(categoryId, boardId);

        if (category.getType() == PostCategory.CategoryType.NOTICE) {
            boardValidator.getBoardIfManager(boardId, authorId);
        }

        String safeContent = htmlSanitizer.sanitize(content);

        Post post = Post.builder()
                .board(board)
                .postCategory(category)
                .author(new Author(authorId, authorNickname))
                .title(title)
                .content(safeContent)
                .build();

        Post saved = postRepository.save(post);
        eventPublisher.publishEvent(new PostChangedEvent(boardId, saved.getId()));

        return PostDto.from(saved);
    }

    /**
     * 게시글 단건 조회 (조회수 증가 포함)
     */
    @Transactional(readOnly = true)
    public Optional<PostDto> findById(Long id) {
        Optional<Post> postOpt = postRepository.findById(id);
        postOpt.ifPresent(post -> postRedisManager.incrementViewCount(post.getId()));
        return postOpt.map(PostDto::from);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostDto update(Long postId, Long authorId, String title, String content) {
        Post post = postValidator.findByIdOrThrow(postId);
        postValidator.validateIsAuthor(post, authorId);

        String safeContent = htmlSanitizer.sanitize(content);

        post.setTitle(title);
        post.setContent(safeContent);
        post.setUpdatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));

        Post updated = postRepository.save(post);
        eventPublisher.publishEvent(new PostChangedEvent(post.getBoard().getId(), postId));

        return PostDto.from(updated);
    }

    /**
     * 게시글 삭제 (본인)
     */
    @Transactional
    public void delete(Long postId, Long authorId) {
        Post post = postValidator.findByIdOrThrow(postId);
        postValidator.validateIsAuthor(post, authorId);
        Long boardId = post.getBoard().getId();

        postRepository.deleteById(postId);
        postRedisManager.deleteViewCache(postId);
        eventPublisher.publishEvent(new PostChangedEvent(boardId, postId));
    }

    /**
     * 게시글 삭제 (관리자)
     */
    @Transactional
    public void deleteForBoardManager(Long postId, Long boardId, Long managerId) {
        postValidator.findByIdOrThrow(postId);
        boardValidator.getBoardIfManager(boardId, managerId);

        postRepository.deleteById(postId);
        postRedisManager.deleteViewCache(postId);
        eventPublisher.publishEvent(new PostChangedEvent(boardId, postId));
    }

    /**
     * 사용자별 작성 게시글 조회
     */
    @Transactional(readOnly = true)
    public Page<PostDto> findByAuthorNickname(String nickname, Pageable pageable) {
        return postRepository.findByAuthorNickname(nickname, pageable).map(PostDto::from);
    }

    /**
     * 전체 조회 (5페이지까지 캐싱)
     */
    private Page<PostDto> getList(Long boardId, Long categoryId, Long threshold, Pageable pageable) {
        if (pageable.getPageNumber()< 5) {
            Optional<CachedPageResponse<PostDto>> cached =
                    postRedisManager.getPostPage(boardId, categoryId, threshold, pageable);

            if (cached.isPresent()) {
                return cached.get().toPage(pageable);
            }
        }

        Page<PostDto> page = postRepository.findPostsFiltered(boardId, categoryId, threshold, pageable)
                .map(PostDto::from);

        if (pageable.getPageNumber() < 5 && !page.isEmpty()) {
            postRedisManager.savePostList(boardId, categoryId, threshold, pageable, CachedPageResponse.of(page));
        }

        return page;
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

    @Transactional
    public void updateViewCountInDb(Long postId, Long viewCount) {
        postRepository.incrementViewCount(postId, viewCount);
    }
}