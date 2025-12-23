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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

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
        return postRepository.findById(id);
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
    }
}