package com.example.galdcup.post;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.post.dto.PostDto;
import com.example.galdcup.post.embedded.Author;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return PostDto.from(postRepository.save(post));
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
}