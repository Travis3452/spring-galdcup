package com.example.galdcup.aiagent;

import com.example.galdcup.comment.domain.Comment;
import com.example.galdcup.comment.domain.CommentRepository;
import com.example.galdcup.post.domain.Post;
import com.example.galdcup.post.domain.PostRepository;
import com.example.galdcup.post.redis.PostRedisManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiAgentWriter {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostRedisManager postRedisManager;

    /**
     * 게시글 일괄 저장 및 캐시 무효화
     */
    @Transactional
    public void savePosts(Long boardId, List<Post> posts) {
        postRepository.saveAll(posts);
        postRedisManager.deletePostListCache(boardId);
    }

    /**
     * 댓글 일괄 저장 및 캐시 무효화
     */
    @Transactional
    public void saveComments(Long boardId, List<Comment> comments) {
        commentRepository.saveAll(comments);
    }
}