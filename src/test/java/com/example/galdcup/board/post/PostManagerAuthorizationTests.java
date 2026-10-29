package com.example.galdcup.board.post;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.board.post.domain.Post;
import com.example.galdcup.board.post.domain.PostReactionRepository;
import com.example.galdcup.board.post.domain.PostRepository;
import com.example.galdcup.board.post.event.PostChangedEvent;
import com.example.galdcup.board.post.redis.PostRedisManager;
import com.example.galdcup.board.post.validator.PostValidator;
import com.example.galdcup.board.postCategory.validator.PostCategoryValidator;
import com.example.galdcup.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.owasp.html.PolicyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostManagerAuthorizationTests {

    private final PostRepository repository = mock(PostRepository.class);
    private final PostRedisManager redis = mock(PostRedisManager.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BoardValidator boards = mock(BoardValidator.class);
    private final PostValidator posts = mock(PostValidator.class);
    private final PostService service = new PostService(repository, redis, events, boards, posts,
            mock(PostCategoryValidator.class), mock(PolicyFactory.class),
            mock(UserValidator.class), mock(PostReactionRepository.class));
    private final Post post = Post.builder().id(100L).board(Board.builder().id(10L).build()).build();

    @Test
    void managerCannotDeletePostFromAnotherBoard() {
        when(posts.findByIdOrThrow(100L)).thenReturn(post);

        assertThrows(AccessDeniedException.class, () -> service.deleteForBoardManager(100L, 20L, 7L));

        verifyNoInteractions(boards, repository, redis, events);
    }

    @Test
    void matchingBoardStillRequiresManagerPermission() {
        when(posts.findByIdOrThrow(100L)).thenReturn(post);
        when(boards.getBoardIfManager(10L, 7L)).thenThrow(new AccessDeniedException("not a manager"));

        assertThrows(AccessDeniedException.class, () -> service.deleteForBoardManager(100L, 10L, 7L));

        verifyNoInteractions(repository, redis, events);
    }

    @Test
    void authorizedDeletionInvalidatesActualBoardsCache() {
        when(posts.findByIdOrThrow(100L)).thenReturn(post);

        service.deleteForBoardManager(100L, 10L, 7L);

        var order = inOrder(boards, repository, redis, events);
        order.verify(boards).getBoardIfManager(10L, 7L);
        order.verify(repository).delete(post);
        order.verify(redis).deleteViewCache(100L);
        ArgumentCaptor<PostChangedEvent> change = ArgumentCaptor.forClass(PostChangedEvent.class);
        order.verify(events).publishEvent(change.capture());
        assertEquals(10L, change.getValue().getBoardId());
        assertEquals(100L, change.getValue().getPostId());
    }
}
