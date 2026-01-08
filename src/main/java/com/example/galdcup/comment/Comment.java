package com.example.galdcup.comment;

import com.example.galdcup.comment.embedded.Author;
import com.example.galdcup.post.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    @Builder.Default
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reply> replies = new ArrayList<>();

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
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

