package com.example.galdcup.post;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.post.dto.PostDto;
import com.example.galdcup.post.embedded.Author;
import com.example.galdcup.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 게시글 생성
     */
    @Transactional
    public PostDto create(Long boardId, Long authorId, String authorNickname, String title, String content) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

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

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

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

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

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
    public PostDto update(Long id, Long authorId, String title, String content) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new AccessDeniedException("이 게시글의 작성자가 아닙니다.");
        }

        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));

        return PostDto.from(postRepository.save(post));
    }

    /**
     * 게시글 삭제(게시판 관리자 전용)
     */
    @Transactional
    public void deleteForBoardManager(Long id, Long boardId, Long managerId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        List<User> subManagers = board.getBoardPolicy().getSubManagers();
        User boardManager = board.getBoardPolicy().getBoardManager();

        if (subManagers.stream().noneMatch(user -> user.getId().equals(managerId))
                && !boardManager.getId().equals(managerId)) {
            throw new AccessDeniedException("게시판 관리자 권한이 필요합니다.");
        }

        postRepository.deleteById(id);
        redisTemplate.delete("post:view:" + id);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void delete(Long id, Long authorId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new AccessDeniedException("이 게시글의 작성자가 아닙니다.");
        }

        postRepository.deleteById(id);
        redisTemplate.delete("post:view:" + id);
    }

    /**
     * 게시판 인기글 목록 조회 (좋아요 수 기준 + 최신순 정렬)
     */
    @Transactional(readOnly = true)
    public Page<PostDto> getPopularPostsByBoard(Long boardId, Pageable pageable) {
        String cacheKey = "posts:popular:board:" + boardId + ":" + pageable.getPageNumber();

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<PostDto> cachedList = mapper.readValue(cached, new TypeReference<List<PostDto>>() {});
                return new PageImpl<>(cachedList, pageable, cachedList.size());
            } catch (Exception e) {
                // 캐시 파싱 실패 시 무시하고 새로 조회
            }
        }

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));
        long likeThreshold = board.getBoardPolicy().getLikeThreshold();

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<PostDto> result = postRepository
                .findByBoardIdAndLikeCountGreaterThanEqual(boardId, likeThreshold, sortedPageable)
                .map(PostDto::from);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(result.getContent());
            redisTemplate.opsForValue().set(cacheKey, json, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 캐시 저장 실패 시 무시
        }

        return result;
    }
}