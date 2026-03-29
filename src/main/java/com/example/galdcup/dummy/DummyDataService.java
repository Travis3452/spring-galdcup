package com.example.galdcup.dummy;

import com.example.galdcup.comment.domain.Comment;
import com.example.galdcup.comment.domain.CommentRepository;
import com.example.galdcup.gemini.GeminiService;
import com.example.galdcup.gemini.response.CommentContextResponse;
import com.example.galdcup.gemini.response.PostContextResponse;
import com.example.galdcup.post.domain.Post;
import com.example.galdcup.post.domain.PostRepository;
import com.example.galdcup.postCategory.domain.PostCategory;
import com.example.galdcup.postCategory.domain.PostCategoryRepository;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.domain.UserRepository;
import com.example.galdcup.vote.domain.VoteOption;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataService {
    private final GeminiService geminiService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostCategoryRepository postCategoryRepository;
    private final VoteSessionValidator voteSessionValidator;

    private final Random random = new Random();

    private static final int DUMMY_USER_START_ID = 9001;
    private static final int DUMMY_USER_SIZE = 1000;

    /**
     * 더미 게시글 30개 생성
     */
    public void generateDummyPosts(Long boardId) {
        VoteSession session = voteSessionValidator.validateAndGetActiveVoteSession(boardId);
        PostCategory generalCategory = postCategoryRepository.findByBoardIdAndName(boardId, "일반")
                .orElseThrow(() -> new IllegalStateException("일반 카테고리가 없습니다."));
        List<String> labels = getLabels(session);

        PostContextResponse ctx = geminiService.getPostContext(
                session.getTopic(), session.getDescription(), labels);

        int postCount = ctx.posts().size();
        List<User> authors = getRandomDummyUsers(postCount);

        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < postCount; i++) {
            var data = ctx.posts().get(i);
            User author = authors.get(i);

            posts.add(Post.builder()
                    .board(session.getBoard())
                    .postCategory(generalCategory)
                    .title(data.title())
                    .content(truncateText(data.content(), 200))
                    .author(new com.example.galdcup.post.domain.embedded.Author(author.getId(), author.getNickname()))
                    .createdAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                    .viewCount(0L)
                    .likeCount(0L)
                    .dislikeCount(0L)
                    .build());
        }

        postRepository.saveAll(posts);
    }

    /**
     * 더미 댓글 20개 생성
     */
    public void generateDummyComments(Long boardId) {
        VoteSession session = voteSessionValidator.validateAndGetActiveVoteSession(boardId);
        List<String> labels = getLabels(session);
        List<Post> targetPosts = postRepository.findByBoardIdOrderByCreatedAtDesc(
                boardId, PageRequest.of(0, 10)).getContent();

        if (targetPosts.isEmpty()) throw new IllegalStateException("댓글을 작성할 게시글이 없습니다.");

        CommentContextResponse ctx = geminiService.getCommentContext(
                session.getTopic(), session.getDescription(), labels);

        List<String> aiComments = ctx.comments();
        int commentCount = aiComments.size();
        List<User> authors = getRandomDummyUsers(commentCount);

        List<Comment> comments = new ArrayList<>();
        int postCount = targetPosts.size();

        for (int i = 0; i < commentCount; i++) {
            Post post = targetPosts.get(i % postCount);
            String content = aiComments.get(i);
            User author = authors.get(i);

            comments.add(Comment.builder()
                    .post(post)
                    .content(truncateText(content, 100))
                    .author(new com.example.galdcup.comment.embedded.Author(author.getId(), author.getNickname()))
                    .createdAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                    .build());
        }

        commentRepository.saveAll(comments);
    }

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

    private String truncateText(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }

    private List<String> getLabels(VoteSession s) {
        return s.getOptions().stream().map(VoteOption::getLabel).toList();
    }
}