package com.example.galdcup.service;

import com.example.galdcup.entity.Board;
import com.example.galdcup.entity.Post;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.BoardRepository;
import com.example.galdcup.repository.PostRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 게시글 생성
    @Transactional
    public Post create(Long boardId, Long authorId, String title, String content) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Post post = Post.builder()
                .board(board)
                .author(author)
                .title(title)
                .content(content)
                .build();

        return postRepository.save(post);
    }

    // 특정 게시판의 게시글 조회 (페이지네이션 적용)
    @Transactional(readOnly = true)
    public Page<Post> findByBoard(Long boardId, Pageable pageable) {
        return postRepository.findByBoardId(boardId, pageable);
    }

    // 특정 사용자의 게시글 조회 (페이지네이션 적용)
    @Transactional(readOnly = true)
    public Page<Post> findByAuthorNickname(String nickname, Pageable pageable) {
        return postRepository.findByAuthorNickname(nickname, pageable);
    }

    // 게시글 조회
    @Transactional(readOnly = true)
    public Optional<Post> findById(Long id) {
        Optional<Post> postOpt = postRepository.findById(id);
        postOpt.ifPresent(post -> incrementViewCount(post.getId()));
        return postOpt;
    }

    // Redis에 조회수 증가 기록
    private void incrementViewCount(Long postId) {
        String key = "post:view:" + postId;
        redisTemplate.opsForValue().increment(key);

        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    // 게시글 수정
    @Transactional
    public Post update(Long id, Long authorId, String title, String content) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new AccessDeniedException("이 게시글의 작성자가 아닙니다.");
        }

        post.setTitle(title);
        post.setContent(content);
        return postRepository.save(post);
    }

    // 게시글 삭제
    @Transactional
    public void delete(Long id, Long authorId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new AccessDeniedException("이 게시글의 작성자가 아닙니다.");
        }

        postRepository.deleteById(id);
        redisTemplate.delete("post:view:" + id); // Redis 캐시도 삭제
    }
}