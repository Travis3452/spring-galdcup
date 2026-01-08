package com.example.galdcup.comment;

import com.example.galdcup.comment.dto.CommentDto;
import com.example.galdcup.comment.embedded.Author;
import com.example.galdcup.post.Post;
import com.example.galdcup.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    /** 게시글별 댓글 조회 */
    @Transactional(readOnly = true)
    public Page<CommentDto> findByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable)
                .map(CommentDto::from);
    }

    /** 작성자 닉네임으로 댓글 조회 */
    @Transactional(readOnly = true)
    public Page<CommentDto> findByAuthorNickname(String nickname, Pageable pageable) {
        return commentRepository.findByAuthorNickname(nickname, pageable)
                .map(CommentDto::from);
    }

    /** 댓글 작성 */
    @Transactional
    public CommentDto create(Long postId, Long authorId, String authorNickname, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .post(post)
                .author(new Author(authorId, authorNickname))
                .content(content)
                .build();

        return CommentDto.from(commentRepository.save(comment));
    }

    /** 댓글 수정 */
    @Transactional
    public CommentDto update(Long id, Long authorId, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (comment.isDeleted()) {
            throw new IllegalStateException("이미 삭제된 댓글입니다.");
        }

        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new SecurityException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        comment.setContent(content);
        return CommentDto.from(commentRepository.save(comment));
    }

    /** 댓글 삭제 */
    @Transactional
    public CommentDto delete(Long id, Long authorId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (comment.isDeleted()) {
            throw new IllegalStateException("이미 삭제된 댓글입니다.");
        }

        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new SecurityException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        comment.delete();
        return CommentDto.from(commentRepository.save(comment));
    }
}