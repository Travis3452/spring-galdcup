package com.example.galdcup.comment;

import com.example.galdcup.comment.embedded.Author;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "replies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reply {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 부모 댓글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment parentComment;

    /** 작성자 정보 */
    @Embedded
    private Author author;

    @Column(nullable = false, length = 300)
    private String content;

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

    /** 대댓글 삭제 처리 (더미로 남겨둠) */
    public void delete() {
        this.deletedAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        this.content = "삭제된 댓글입니다.";
        this.author = new Author(null, "알 수 없는 사용자");
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
