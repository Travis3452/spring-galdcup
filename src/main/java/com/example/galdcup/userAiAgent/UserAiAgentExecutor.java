package com.example.galdcup.userAiAgent;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.comment.CommentService;
import com.example.galdcup.board.comment.domain.Comment;
import com.example.galdcup.board.comment.domain.CommentRepository;
import com.example.galdcup.board.comment.request.CreateCommentRequest;
import com.example.galdcup.board.post.PostService;
import com.example.galdcup.board.post.domain.Post;
import com.example.galdcup.board.post.domain.PostRepository;
import com.example.galdcup.board.postCategory.domain.PostCategory;
import com.example.galdcup.board.postCategory.validator.PostCategoryValidator;
import com.example.galdcup.userAiAgent.domain.UserAiAgent;
import com.example.galdcup.userAiAgent.gemini.UserAiAgentGeminiClient;
import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentCommentResponse;
import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentPostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserAiAgentExecutor {

    private final UserAiAgentGeminiClient geminiClient;
    private final PostService postService;
    private final CommentService commentService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostCategoryValidator postCategoryValidator;

    /**
     * AI 용병 게시글 작성 실행
     */
    @Transactional
    public void executePostCreation(UserAiAgent agent, String rawApiKey) {
        Board targetBoard = agent.getTargetBoard();
        Long boardId = targetBoard.getId();
        Long authorId = agent.getUser().getId();

        PostCategory generalCategory = postCategoryValidator.findGeneralCategoryOrThrowByBoardId(boardId);

        // 1. 게시판 인기 게시글 5개 조회 (제목 + 본문)
        List<String> popularPosts = postRepository.findTop5ByBoardIdOrderByLikeCountDescCreatedAtDesc(boardId)
                .stream()
                .map(post -> String.format("제목: %s | 내용: %s", post.getTitle(), post.getContent()))
                .toList();

        // 2. 게시판 최신 게시글 5개 조회 (제목 + 본문)
        List<String> latestPosts = postRepository.findTop5ByBoardIdOrderByCreatedAtDesc(boardId)
                .stream()
                .map(post -> String.format("제목: %s | 내용: %s", post.getTitle(), post.getContent()))
                .toList();

        // 3. 본인(용병)이 최근 작성한 과거 게시글 10개 조회 (제목 + 본문)
        List<String> myPastPosts = postRepository.findTop10ByAuthorIdOrderByCreatedAtDesc(authorId)
                .stream()
                .map(post -> String.format("제목: %s | 내용: %s", post.getTitle(), post.getContent()))
                .toList();

        // 4. Gemini API 호출
        UserAiAgentPostResponse postResponse = geminiClient.generatePost(
                rawApiKey,
                agent.getPersonaPrompt(),
                targetBoard.getTopic(),
                popularPosts,
                latestPosts,
                myPastPosts
        );

        // 5. 게시글 등록
        postService.create(
                boardId,
                generalCategory.getId(),
                authorId,
                postResponse.title(),
                postResponse.content()
        );

        // 6. 실행 시각 업데이트
        agent.updateLastExecutedAt();
    }

    /**
     * AI 용병 댓글 작성 실행
     */
    @Transactional
    public void executeCommentCreation(UserAiAgent agent, String rawApiKey) {
        Board targetBoard = agent.getTargetBoard();
        Long authorId = agent.getUser().getId();

        // 1. 댓글을 작성할 대상 최신 게시글 조회
        Post targetPost = postRepository.findTopByBoardIdOrderByCreatedAtDesc(targetBoard.getId())
                .orElseThrow(() -> new IllegalStateException("댓글을 작성할 최신 게시글이 없습니다."));

        // 2. 해당 게시글에 이미 달린 최근 댓글 5개 조회 (타인 멘트 중복 방지)
        List<String> recentCommentsOnPost = commentRepository.findTop5ByPostIdOrderByCreatedAtDesc(targetPost.getId())
                .stream()
                .map(Comment::getContent)
                .toList();

        // 3. 본인(용병)이 과거에 작성했던 최근 댓글 10개 조회 (자기 복제 방지)
        List<String> myPastComments = commentRepository.findTop10ByAuthorIdOrderByCreatedAtDesc(authorId)
                .stream()
                .map(Comment::getContent)
                .toList();

        // 4. Gemini API 호출
        UserAiAgentCommentResponse commentResponse = geminiClient.generateComment(
                rawApiKey,
                agent.getPersonaPrompt(),
                targetPost.getTitle(),
                targetPost.getContent(),
                recentCommentsOnPost,
                myPastComments
        );

        // 5. 댓글 등록
        CreateCommentRequest request = new CreateCommentRequest(null, commentResponse.content());
        commentService.create(targetPost.getId(), authorId, request);

        // 6. 실행 시각 업데이트 (Dirty Checking 정상 동작)
        agent.updateLastExecutedAt();
    }
}