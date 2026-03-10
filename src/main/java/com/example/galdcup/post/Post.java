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
                @Index(name = "idx_post_board_id", columnList = "board_id"),
                @Index(name = "idx_post_view_count", columnList = "viewCount DESC"),
                @Index(name = "idx_post_board_created", columnList = "board_id, createdAt DESC"),
                @Index(name = "idx_post_board_category_created", columnList = "board_id, post_category_id, createdAt DESC")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_category_id", nullable = false)
    private PostCategory postCategory;

    @Embedded
    private Author author;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false)
    private long likeCount = 0;

    @Column(nullable = false)
    private long dislikeCount = 0;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<PostReaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        createdAt = now;
        updatedAt = now;
    }

    public void addLike() { this.likeCount++; }
    public void addDislike() { this.dislikeCount++; }
}