package com.example.galdcup.post;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.post.dto.PostDto;
import com.example.galdcup.post.dto.PostListResponse;
import com.example.galdcup.post.embedded.Author;
import com.example.galdcup.post.validator.PostValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private final BoardValidator boardValidator;
    private final PostValidator postValidator;

    /**
     * 게시글 생성
     */
    @Transactional
    public PostDto create(Long boardId, Long authorId, String authorNickname, String title, String content) {
        Board board = boardValidator.validateAndGetActiveBoard(boardId);

        Post post = Post.builder()
                .board(board)
                .author(new Author(authorId, authorNickname))
                .title(title)
                .content(content)
                .build();

        return PostDto.from(postRepository.save(post));
    }

    /**
     * 게시판별 게시글 조회(최신순)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> findByBoard(Long boardId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return postRepository.findByBoardId(boardId, sortedPageable)
                .map(PostDto::from);
    }

    /**
     * 게시판의 게시글 검색(제목+내용)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getPostsByTitleAndContent(Pageable pageable, Long boardId, String keyword) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return postRepository.searchByBoardAndTitleOrContent(boardId, keyword, sortedPageable)
                .map(PostDto::from);
    }

    /**
     * 게시판의 게시글 검색(작성자)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getPostsByAuthorNickname(Pageable pageable, Long boardId, String keyword) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return postRepository.searchByBoardAndAuthor(boardId, keyword, sortedPageable)
                .map(PostDto::from);
    }

    /**
     * 게시판의 인기글 검색(제목+내용)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getPopularPostsByTitleAndContent(Pageable pageable, Long boardId, String keyword) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Board board = boardValidator.validateAndGetBoard(boardId);

        Long likeThreshold = board.getBoardPolicy().getLikeThreshold();

        return postRepository.searchPopularPostsByBoardAndTitleOrContent(boardId, likeThreshold, keyword, sortedPageable)
                .map(PostDto::from);
    }

    /**
     * 게시판의 인기글 검색(작성자)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getPopularPostsByAuthorNickname(Pageable pageable, Long boardId, String keyword) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Board board = boardValidator.validateAndGetBoard(boardId);

        Long likeThreshold = board.getBoardPolicy().getLikeThreshold();

        return postRepository.searchPopularPostsByBoardAndTitleOrContent(boardId, likeThreshold, keyword, sortedPageable)
                .map(PostDto::from);
    }
    
    /**
     * 사용자별 게시글 조회(최신순)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> findByAuthorNickname(String nickname, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return postRepository.findByAuthorNickname(nickname, sortedPageable)
                .map(PostDto::from);
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
     * Redis 조회수 증가 기록
     */
    private void incrementViewCount(Long postId) {
        String key = "post:view:" + postId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostDto update(Long postId, Long authorId, String title, String content) {
        Post post = postValidator.validateAndGetPost(postId);

        postValidator.validatePostAuthor(post, authorId);

        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));

        return PostDto.from(postRepository.save(post));
    }

    /**
     * 게시글 삭제(게시판 관리자 전용)
     */
    @Transactional
    public void deleteForBoardManager(Long postId, Long boardId, Long managerId) {
        Post post = postValidator.validateAndGetPost(postId);

        Board board = boardValidator.validateAndGetBoard(boardId);

        boardValidator.checkManagerAuthority(board, managerId);

        postRepository.deleteById(postId);
        redisTemplate.delete("post:view:" + postId);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void delete(Long postId, Long authorId) {
        Post post = postValidator.validateAndGetPost(postId);

        postValidator.validatePostAuthor(post, authorId);

        postRepository.deleteById(postId);
        redisTemplate.delete("post:view:" + postId);
    }

    /**
     * 게시판 인기글 목록 조회 (좋아요 수 기준 + 최신순 정렬)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getPopularPostsByBoard(Long boardId, Pageable pageable) {
        String cacheKey = "posts:popular:board:" + boardId + ":" + pageable.getPageNumber();

        PostListResponse cached = (PostListResponse) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return new PageImpl<>(cached.getPostDtos(), pageable, cached.getPostDtos().size());
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Board board = boardValidator.validateAndGetBoard(boardId);
        long likeThreshold = board.getBoardPolicy().getLikeThreshold();

        Page<PostDto> result = postRepository
                .findByBoardIdAndLikeCountGreaterThanEqual(boardId, likeThreshold, sortedPageable)
                .map(PostDto::from);

        PostListResponse responseToCache = new PostListResponse(result.getContent());

        redisTemplate.opsForValue().set(cacheKey, responseToCache, Duration.ofSeconds(10));

        return result;
    }
}