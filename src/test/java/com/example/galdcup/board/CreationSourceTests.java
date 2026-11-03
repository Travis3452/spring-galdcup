package com.example.galdcup.board;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.board.comment.CommentService;
import com.example.galdcup.board.comment.domain.Comment;
import com.example.galdcup.board.comment.domain.CommentRepository;
import com.example.galdcup.board.comment.request.CreateCommentRequest;
import com.example.galdcup.board.comment.validator.CommentValidator;
import com.example.galdcup.board.post.PostService;
import com.example.galdcup.board.post.domain.Post;
import com.example.galdcup.board.post.domain.PostReactionRepository;
import com.example.galdcup.board.post.domain.PostRepository;
import com.example.galdcup.board.post.redis.PostRedisManager;
import com.example.galdcup.board.post.validator.PostValidator;
import com.example.galdcup.board.postCategory.domain.PostCategory;
import com.example.galdcup.board.postCategory.validator.PostCategoryValidator;
import com.example.galdcup.common.CreationSource;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.owasp.html.PolicyFactory;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CreationSourceTests {
    private final Board board = Board.builder().id(1L).build();
    private final User user = User.builder().id(7L).nickname("member").build();
    private final PostCategory category = PostCategory.builder().id(2L).board(board)
            .type(PostCategory.CategoryType.GENERAL).name("General").build();

    @Test
    void postCreationPathsPersistAndReturnDifferentSources() {
        var repository = mock(PostRepository.class);
        var boards = mock(BoardValidator.class);
        var categories = mock(PostCategoryValidator.class);
        var users = mock(UserValidator.class);
        var sanitizer = mock(PolicyFactory.class);
        when(boards.findActiveBoardByIdOrThrow(1L)).thenReturn(board);
        when(categories.findCategoryInBoardOrThrow(2L, 1L)).thenReturn(category);
        when(users.findByIdOrThrow(7L)).thenReturn(user);
        when(sanitizer.sanitize("body")).thenReturn("safe");
        when(repository.save(any(Post.class))).thenAnswer(call -> call.getArgument(0));
        var guard = mock(com.example.galdcup.board.board.ban.BoardWriteGuard.class);
        var service = new PostService(repository, mock(PostRedisManager.class), mock(ApplicationEventPublisher.class),
                boards, mock(PostValidator.class), categories, sanitizer, users, guard, mock(PostReactionRepository.class));
        assertEquals(CreationSource.USER, service.create(1L, 2L, 7L, "title", "body").creationSource());
        assertEquals(CreationSource.AI, service.createAiPost(1L, 2L, 7L, "title", "body").creationSource());
        verify(repository).save(argThat(post -> post.getCreationSource() == CreationSource.AI && post.getContent().equals("safe")));
        doThrow(new org.springframework.security.access.AccessDeniedException("banned")).when(guard).requireAllowed(1L,7L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.create(1L,2L,7L,"title","body"));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.createAiPost(1L,2L,7L,"title","body"));
        verify(repository,times(2)).save(any());
    }

    @Test
    void commentCreationPathsIncludeRepliesAndReturnSource() {
        var posts = mock(PostValidator.class);
        var users = mock(UserValidator.class);
        var comments = mock(CommentValidator.class);
        var repository = mock(CommentRepository.class);
        var post = Post.create(board, category, user, "title", "body");
        var parent = Comment.create(post, user, "parent", null);
        when(posts.findByIdOrThrow(3L)).thenReturn(post);
        when(users.findByIdOrThrow(7L)).thenReturn(user);
        when(comments.getValidatedParentComment(4L, post.getId())).thenReturn(parent);
        when(repository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));
        var guard = mock(com.example.galdcup.board.board.ban.BoardWriteGuard.class);
        var service = new CommentService(posts, comments, repository, users, guard);
        assertEquals(CreationSource.USER, service.create(3L, 7L, new CreateCommentRequest(4L, "reply")).creationSource());
        assertEquals(CreationSource.AI, service.createAiComment(3L, 7L, new CreateCommentRequest(null, "ai")).creationSource());
        assertEquals(1, parent.getChildrenComments().size());
        doThrow(new org.springframework.security.access.AccessDeniedException("banned")).when(guard).requireAllowed(1L,7L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.create(3L,7L,new CreateCommentRequest(4L,"reply")));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.createAiComment(3L,7L,new CreateCommentRequest(null,"ai")));
        verify(repository,times(2)).save(any());
    }

    @Test
    void editsPreserveOriginalSourceAndLegacyBuildersRemainUnknown() {
        var post = Post.create(board, category, user, "title", "body", CreationSource.AI);
        post.update("edited", "edited", category);
        post.updateByManager("moderated", "moderated", category);
        assertEquals(CreationSource.AI, post.getCreationSource());
        var comment = Comment.create(post, user, "body", null, CreationSource.AI);
        comment.update("edited");
        comment.delete();
        assertEquals(CreationSource.AI, comment.getCreationSource());
        assertEquals(CreationSource.UNKNOWN, Post.builder().build().getCreationSource());
        assertEquals(CreationSource.UNKNOWN, Comment.builder().build().getCreationSource());
    }
}
