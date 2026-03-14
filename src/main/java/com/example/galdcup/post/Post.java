package com.example.galdcup.post;

import com.example.galdcup.board.Board;
import com.example.galdcup.comment.Comment;
import com.example.galdcup.post.embedded.Author;
import com.example.galdcup.postCategory.PostCategory;
import com.example.galdcup.postReaction.PostReaction;
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
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    public void addLike() { this.likeCount++; }
    public void addDislike() { this.dislikeCount++; }
}