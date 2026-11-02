package com.example.galdcup.userAiAgent;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.comment.CommentService;
import com.example.galdcup.board.comment.domain.CommentRepository;
import com.example.galdcup.board.post.PostService;
import com.example.galdcup.board.post.domain.Post;
import com.example.galdcup.board.post.domain.PostRepository;
import com.example.galdcup.board.postCategory.validator.PostCategoryValidator;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.userAiAgent.domain.AgentMode;
import com.example.galdcup.userAiAgent.domain.UserAiAgent;
import com.example.galdcup.userAiAgent.domain.UserAiAgentRepository;
import com.example.galdcup.userAiAgent.gemini.UserAiAgentGeminiClient;
import com.example.galdcup.userAiAgent.gemini.response.UserAiAgentCommentResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserAiAgentCommentTests {
    final UserAiAgentGeminiClient gemini = mock(UserAiAgentGeminiClient.class);
    final PostService postService = mock(PostService.class);
    final CommentService commentService = mock(CommentService.class);
    final PostRepository posts = mock(PostRepository.class);
    final CommentRepository comments = mock(CommentRepository.class);
    final UserAiAgentRepository agents = mock(UserAiAgentRepository.class);
    final UserAiAgentExecutor executor = new UserAiAgentExecutor(gemini, postService, commentService,
            posts, comments, mock(PostCategoryValidator.class), agents);
    final UserAiAgent agent = UserAiAgent.builder().id(9L).user(User.builder().id(7L).build())
            .targetBoard(Board.builder().id(1L).build()).agentMode(AgentMode.COMMENT)
            .isActive(true).intervalMinutes(30).expiredAt(OffsetDateTime.now().plusHours(1))
            .lastExecutedAt(OffsetDateTime.now().minusHours(1)).personaPrompt("persona").build();
    final Post post = Post.builder().id(104L).title("title").content("body").build();

    @Test
    void missingAgentDoesNotGenerateAnything() {
        executor.executeCommentCreation(agent, "unused");
        verifyNoInteractions(posts, comments, gemini, commentService);
    }

    @Test
    void inactiveOrExpiredAgentDoesNotGenerateAnything() {
        when(agents.findByIdForCommentUpdate(9L)).thenReturn(Optional.of(agent));
        agent.toggleActive(false);
        executor.executeCommentCreation(agent, "unused");
        var expired = UserAiAgent.builder().id(9L).isActive(true).expiredAt(OffsetDateTime.now().minusMinutes(1)).build();
        when(agents.findByIdForCommentUpdate(9L)).thenReturn(Optional.of(expired));
        executor.executeCommentCreation(agent, "unused");
        verifyNoInteractions(posts, comments, gemini, commentService);
    }

    @Test
    void noEligiblePostSkipsGeminiAndDoesNotChangeLastWriteTime() {
        when(agents.findByIdForCommentUpdate(9L)).thenReturn(Optional.of(agent));
        var before = agent.getLastExecutedAt();
        executor.executeCommentCreation(agent, "unused");
        verifyNoInteractions(comments, gemini, commentService);
        assertEquals(before, agent.getLastExecutedAt());
        assertTrue(agent.getCommentedPosts().isEmpty());
        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(agents).findUncommentedPostsForAgent(eq(1L), eq(9L), eq(7L), page.capture());
        assertEquals(1, page.getValue().getPageSize());
    }

    @Test
    void successfulCommentRecordsTheLockedAgentsTarget() {
        var detached = UserAiAgent.builder().id(9L).build();
        when(agents.findByIdForCommentUpdate(9L)).thenReturn(Optional.of(agent));
        when(agents.findUncommentedPostsForAgent(eq(1L), eq(9L), eq(7L), any())).thenReturn(List.of(post));
        when(gemini.generateComment(eq("unused"), eq("persona"), eq("title"), eq("body"), anyList(), anyList()))
                .thenReturn(new UserAiAgentCommentResponse("new comment"));
        executor.executeCommentCreation(detached, "unused");
        verify(commentService).createAiComment(eq(104L), eq(7L), argThat(request -> request.content().equals("new comment")));
        assertEquals(1, agent.getCommentedPosts().size());
        assertTrue(agent.getCommentedPosts().contains(post));
        assertNull(detached.getLastExecutedAt());
    }

    @Test
    void failedGenerationOrSaveDoesNotRecordTheTarget() {
        when(agents.findByIdForCommentUpdate(9L)).thenReturn(Optional.of(agent));
        when(agents.findUncommentedPostsForAgent(eq(1L), eq(9L), eq(7L), any())).thenReturn(List.of(post));
        when(gemini.generateComment(anyString(), anyString(), anyString(), anyString(), anyList(), anyList()))
                .thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> executor.executeCommentCreation(agent, "unused"));
        assertTrue(agent.getCommentedPosts().isEmpty());
        doReturn(new UserAiAgentCommentResponse("new comment")).when(gemini)
                .generateComment(anyString(), anyString(), anyString(), anyString(), anyList(), anyList());
        doThrow(new IllegalStateException("save failed")).when(commentService).createAiComment(anyLong(), anyLong(), any());
        assertThrows(IllegalStateException.class, () -> executor.executeCommentCreation(agent, "unused"));
        assertTrue(agent.getCommentedPosts().isEmpty());
    }

    @Test
    void skippedExecutionStillUsesTheExistingNormalInterval() {
        var encryption = mock(com.example.galdcup.common.util.ApiKeyEncryptor.class);
        var redis = mock(com.example.galdcup.userAiAgent.redis.UserAiAgentRedisManager.class);
        when(encryption.decrypt(any())).thenReturn("unused");
        when(agents.findByIdForCommentUpdate(9L)).thenReturn(Optional.of(agent));
        var executionService = new UserAiAgentExecutionService(encryption, redis, executor);
        executionService.execute(agent);
        verify(redis).setCooldown(9L, 1800L);
        verify(redis, never()).setCooldown(9L, 300L);
        verifyNoInteractions(gemini, commentService);
    }
}
