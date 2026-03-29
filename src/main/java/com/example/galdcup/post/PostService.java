package com.example.galdcup.post;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.common.redis.CachedPageResponse;
import com.example.galdcup.post.domain.Post;
import com.example.galdcup.post.domain.PostReaction;
import com.example.galdcup.post.domain.PostReactionRepository;
import com.example.galdcup.post.domain.PostRepository;
import com.example.galdcup.post.event.PostChangedEvent;
import com.example.galdcup.post.redis.PostRedisManager;
import com.example.galdcup.post.response.PostDto;
import com.example.galdcup.post.validator.PostValidator;
import com.example.galdcup.postCategory.domain.PostCategory;
import com.example.galdcup.postCategory.validator.PostCategoryValidator;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.PolicyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserValidator userValidator;
    private final PostReactionRepository postReactionRepository;

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
    public PostDto create(Long boardId, Long categoryId, Long authorId, String title, String content) {
        Board board = boardValidator.findActiveBoardByIdOrThrow(boardId);
        User author = userValidator.findByIdOrThrow(authorId);
        PostCategory category = postCategoryValidator.getIfBelongsToBoard(categoryId, boardId);

        if (category.getType() == PostCategory.CategoryType.NOTICE) {
            boardValidator.getBoardIfManager(boardId, author.getId());
        }

        String safeContent = htmlSanitizer.sanitize(content);

        Post post = Post.create(board, category, author, title, safeContent);

        Post saved = postRepository.save(post);
        eventPublisher.publishEvent(new PostChangedEvent(boardId, saved.getId()));

        return PostDto.from(saved);
    }

    /**
     * 게시글 단건 조회 (조회수 증가 포함)
     */
    @Transactional(readOnly = true)
    public PostDto findById(Long postId) {
        Optional<PostDto> cachedPostDto = postRedisManager.getPostDetail(postId);
        postRedisManager.incrementViewCount(postId);

        if (cachedPostDto.isPresent()) {
            return cachedPostDto.get();
        }

        Post post = postValidator.findPostWithCategoryOrThrow(postId);
        PostDto dto = PostDto.from(post);
        postRedisManager.savePostDetail(dto);
        return dto;
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostDto update(Long postId, Long categoryId, Long authorId, String title, String content) {
        Post post = postValidator.findPostWithDetailsOrThrow(postId);
        User user = userValidator.findByIdOrThrow(authorId);

        postValidator.validateIsAuthor(post, authorId);

        PostCategory newCategory = postCategoryValidator.getIfBelongsToBoard(categoryId, post.getBoard().getId());
        String safeContent = htmlSanitizer.sanitize(content);

        boolean isManager = post.getBoard().getBoardPolicy().isBoardManager(user);

        if (isManager) {
            post.updateByManager(title, safeContent, newCategory);
        } else {
            post.update(title, safeContent, newCategory);
        }

        eventPublisher.publishEvent(new PostChangedEvent(post.getBoard().getId(), postId));
        return PostDto.from(post);
    }

    /**
     * 게시글 삭제 (본인)
     */
    @Transactional
    public void delete(Long postId, Long authorId) {
        Post post = postValidator.findByIdOrThrow(postId);
        postValidator.validateIsAuthor(post, authorId);
        Long boardId = post.getBoard().getId();

        postRepository.delete(post);
        postRedisManager.deleteViewCache(postId);
        eventPublisher.publishEvent(new PostChangedEvent(boardId, postId));
    }

    /**
     * 게시글 삭제 (관리자)
     */
    @Transactional
    public void deleteForBoardManager(Long postId, Long boardId, Long managerId) {
        Post post = postValidator.findByIdOrThrow(postId);
        boardValidator.getBoardIfManager(boardId, managerId);

        postRepository.delete(post);
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
        if (pageable.getPageNumber() < 5) {
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

    /**
     * 반응(좋아요/싫어요) 추가
     */
    @Transactional
    public void addReaction(Long postId, Long currentUserId, PostReaction.ReactionType type) {
        // 단순 존재 여부와 좋아요 추가를 위해 가벼운 기본 조회 사용
        Post post = postValidator.findByIdOrThrow(postId);
        User user = userValidator.findByIdOrThrow(currentUserId);

        if (postReactionRepository.findByPostAndUser(post, user).isPresent()) {
            throw new IllegalStateException("이미 좋아요/싫어요를 남긴 게시물입니다.");
        }

        PostReaction reaction = PostReaction.create(post, user, type);
        postReactionRepository.save(reaction);

        post.addReaction(reaction);
    }

    @Transactional
    public void updateViewCountInDb(Long postId, Long viewCount) {
        postRepository.incrementViewCount(postId, viewCount);
    }
}