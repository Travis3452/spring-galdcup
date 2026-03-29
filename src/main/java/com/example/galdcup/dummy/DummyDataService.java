package com.example.galdcup.dummy;

import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.comment.domain.Comment;
import com.example.galdcup.comment.domain.CommentRepository;
import com.example.galdcup.gemini.GeminiService;
import com.example.galdcup.gemini.response.CommentContextResponse;
import com.example.galdcup.gemini.response.PostContextResponse;
import com.example.galdcup.post.domain.Post;
import com.example.galdcup.post.domain.PostRepository;
import com.example.galdcup.postCategory.domain.PostCategory;
import com.example.galdcup.postCategory.validator.PostCategoryValidator;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.domain.UserRepository;
import com.example.galdcup.vote.domain.VoteOption;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataService {
    private final GeminiService geminiService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostCategoryValidator postCategoryValidator;
    private final VoteSessionValidator voteSessionValidator;
    private final BoardValidator boardValidator;
    private final DummyDataWriter dummyDataWriter;

    private final Random random = new Random();
    private static final int DUMMY_USER_START_ID = 9001;
    private static final int DUMMY_USER_SIZE = 1000;

    /**
     * 더미 게시글 생성
     */
    public void generateDummyPosts(Long boardId, Long userId) {
        // 권한 검증
        boardValidator.getBoardIfBoardManager(boardId, userId);

        VoteSession session = voteSessionValidator.validateAndGetActiveVoteSession(boardId);
        PostCategory generalCategory = postCategoryValidator.findGeneralCategoryOrThrowByBoardId(boardId);

        // Gemini API로 게시글 데이터 생성
        PostContextResponse ctx = geminiService.getPostContext(
                session.getTopic(), session.getDescription(), getLabels(session),
                getLatestPostTitle(boardId));

        List<User> authors = getRandomDummyUsers(ctx.posts().size());
        List<Post> posts = new ArrayList<>();

        for (int i = 0; i < ctx.posts().size(); i++) {
            posts.add(Post.create(
                    session.getBoard(),
                    generalCategory,
                    authors.get(i),
                    ctx.posts().get(i).title(),
                    truncateText(ctx.posts().get(i).content(), 200)
            ));
        }

        dummyDataWriter.savePosts(boardId, posts);
    }

    /**
     * 더미 댓글 생성
     */
    public void generateDummyComments(Long boardId, Long userId) {
        boardValidator.getBoardIfBoardManager(boardId, userId);

        VoteSession session = voteSessionValidator.validateAndGetActiveVoteSession(boardId);
        List<Post> targetPosts = postRepository.findTop10ByBoardIdOrderByCreatedAtDesc(boardId);

        if (targetPosts.isEmpty()) throw new IllegalStateException("댓글을 작성할 게시글이 없습니다.");

        // AI 컨텍스트 생성
        CommentContextResponse ctx = geminiService.getCommentContext(
                session.getTopic(), session.getDescription(), getLabels(session),
                getLatestCommentContent(boardId));

        List<User> authors = getRandomDummyUsers(ctx.comments().size());
        int postCount = targetPosts.size();
        List<Comment> comments = new ArrayList<>();

        for (int i = 0; i < ctx.comments().size(); i++) {
            comments.add(Comment.create(
                    targetPosts.get(i % postCount),
                    authors.get(i),
                    truncateText(ctx.comments().get(i), 100),
                    null
            ));
        }

        dummyDataWriter.saveComments(boardId, comments);
    }

    // --- Helper Methods ---

    @Transactional(readOnly = true)
    protected List<User> getRandomDummyUsers(int count) {
        Set<Long> randomIds = new HashSet<>();
        while (randomIds.size() < count) {
            long randomId = DUMMY_USER_START_ID + random.nextInt(DUMMY_USER_SIZE);
            randomIds.add(randomId);
        }
        List<User> users = userRepository.findAllById(randomIds);
        Collections.shuffle(users);
        return users;
    }

    private String getLatestPostTitle(Long boardId) {
        return postRepository.findTopByBoardIdOrderByCreatedAtDesc(boardId)
                .map(Post::getTitle).orElse("새로운 논쟁의 시작");
    }

    private String getLatestCommentContent(Long boardId) {
        return commentRepository.findTopByPostBoardIdOrderByCreatedAtDesc(boardId)
                .map(Comment::getContent).orElse("열띤 토론을 기대합니다");
    }

    private String truncateText(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }

    private List<String> getLabels(VoteSession s) {
        return s.getOptions().stream().map(VoteOption::getLabel).toList();
    }
}