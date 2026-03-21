package com.example.galdcup.comment;

import com.example.galdcup.comment.embedded.Author;
import com.example.galdcup.post.domain.Post;
import com.example.galdcup.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comment_post_id", columnList = "post_id"),
                @Index(name = "idx_comment_post_created_at", columnList = "post_id, createdAt")
        }
)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Embedded
    private Author author;

    @Column(nullable = false, length = 300)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> childrenComments = new ArrayList<>();

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        createdAt = now;
        updatedAt = now;
    }

    public static Comment create(Post post, User user, String content, Comment parentComment) {
        if (parentComment != null && parentComment.getParentComment() != null) {
            throw new IllegalArgumentException("댓글에만 대댓글을 달 수 있습니다.");
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(new Author(user.getId(), user.getNickname()))
                .content(content)
                .parentComment(parentComment)
                .build();

        if (parentComment != null) {
            parentComment.getChildrenComments().add(comment);
        }

        return comment;
    }

    public void update(String content) {
        this.content = content;
        updatedAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void delete() {
        this.deletedAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        this.content = "삭제된 댓글입니다.";
        this.author = new Author(null, "알 수 없는 사용자");
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

}