package com.example.galdcup.comment;

import com.example.galdcup.comment.dto.CommentDto;
import com.example.galdcup.comment.embedded.Author;
import com.example.galdcup.comment.validator.CommentValidator;
import com.example.galdcup.post.Post;
import com.example.galdcup.post.validator.PostValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostValidator postValidator;
    private final CommentValidator commentValidator;
    private final CommentRepository commentRepository;

    /** 게시글별 댓글 조회 (오래된 순 정렬) */
    @Transactional(readOnly = true)
    public Page<CommentDto> findByPost(Long postId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        return commentRepository.findByPostId(postId, sortedPageable)
                .map(CommentDto::from);
    }

    /** 작성자 닉네임으로 댓글 조회 (오래된 순 정렬) */
    @Transactional(readOnly = true)
    public Page<CommentDto> findByAuthorNickname(String nickname, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        return commentRepository.findByAuthorNickname(nickname, sortedPageable)
                .map(CommentDto::from);
    }

    /** 댓글 작성 */
    @Transactional
    public CommentDto create(Long postId, Long authorId, String authorNickname, String content) {
        Post post = postValidator.validateAndGetPost(postId);

        Comment comment = Comment.builder()
                .post(post)
                .author(new Author(authorId, authorNickname))
                .content(content)
                .build();

        return CommentDto.from(commentRepository.save(comment));
    }

    /** 댓글 수정 */
    @Transactional
    public CommentDto update(Long commentId, Long authorId, String content) {
        Comment comment = commentValidator.validateAndGetComment(commentId);
        commentValidator.validateNotDeleted(comment);
        commentValidator.validateIsAuthor(comment, authorId);

        comment.setContent(content);
        return CommentDto.from(comment);
    }

    /** 댓글 삭제 */
    @Transactional
    public CommentDto delete(Long commentId, Long authorId) {
        Comment comment = commentValidator.validateAndGetComment(commentId);
        commentValidator.validateNotDeleted(comment);
        commentValidator.validateIsAuthor(comment, authorId);

        comment.delete();
        return CommentDto.from(comment);
    }
}