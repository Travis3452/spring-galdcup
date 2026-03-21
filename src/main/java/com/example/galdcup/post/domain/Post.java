package com.example.galdcup.post.domain;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.comment.Comment;
import com.example.galdcup.post.domain.embedded.Author;
import com.example.galdcup.postCategory.domain.PostCategory;
import com.example.galdcup.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_post_list", columnList = "board_id, post_category_id, createdAt DESC"),
                @Index(name = "idx_post_popular", columnList = "board_id, likeCount DESC, createdAt DESC"),
                @Index(name = "idx_post_author", columnList = "board_id, author_nickname, createdAt DESC")
        }
)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_category_id", nullable = false)
    private PostCategory postCategory;

    @Embedded
    private Author author;

    @Column(nullable = false, length = 50)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Builder.Default
    @Column(nullable = false)
    private long viewCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private long likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private long dislikeCount = 0;

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PostReaction> reactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    /**
     * 게시글 생성 정적 팩토리 메서드
     */
    public static Post create(Board board, PostCategory category, User user, String title, String content) {
        return Post.builder()
                .board(board)
                .postCategory(category)
                .author(Author.from(user))
                .title(title)
                .content(content)
                .build();
    }

    /**
     * 게시글 수정 (제목, 내용, 카테고리 변경 포함)
     */
    public void update(String title, String content, PostCategory newCategory) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목은 필수입니다.");

        if (this.postCategory.getType() == PostCategory.CategoryType.NOTICE ||
                newCategory.getType() == PostCategory.CategoryType.NOTICE) {

            throw new IllegalArgumentException("공지사항 카테고리는 다른 카테고리로 변경할 수 없습니다.");
        }

        this.title = title;
        this.content = content;
        this.postCategory = newCategory;
        this.updatedAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /**
     * [관리자 전용] 게시글 수정 (카테고리 제약 없음)
     */
    public void updateByManager(String title, String content, PostCategory newCategory) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }

        this.title = title;
        this.content = content;
        this.postCategory = newCategory;
        this.updatedAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /**
     * 게시글 좋아요/싫어요
     */
    public void addReaction(PostReaction reaction) {
        if (reaction.getType() == PostReaction.ReactionType.LIKE) {
            this.likeCount++;
        } else {
            this.dislikeCount++;
        }
    }
}