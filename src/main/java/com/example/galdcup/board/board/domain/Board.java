package com.example.galdcup.board.board.domain;

import com.example.galdcup.board.post.domain.Post;
import com.example.galdcup.board.postCategory.domain.PostCategory;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 특정 주제에 대한 투표와 커뮤니티 활동이 이루어지는 '갈드컵 게시판' 엔티티.
 */
@Entity
@Table(name = "boards")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Board {

    public enum Status { OPEN, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(length = 1000)
    private String description;

    @OneToOne(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private BoardPolicy boardPolicy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostCategory> postCategories = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VoteSession> voteSessions = new ArrayList<>();

    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        if (this.status == null) {
            this.status = Status.OPEN;
        }
    }

    /** 신규 게시판 생성 시 기본 상태는 OPEN. */
    public static Board create(String topic, String description) {
        return Board.builder()
                .topic(topic)
                .description(description)
                .status(Status.OPEN)
                .build();
    }

    public void changeStatus(Status newStatus) {
        this.status = newStatus;
    }

    /** 게시판을 폐쇄 상태로 변경. */
    public void closeBoard() {
        this.status = Status.CLOSED;
    }

    public void assignPolicy(BoardPolicy policy) {
        this.boardPolicy = policy;
    }

    /** 게시판에 카테고리를 추가. */
    public void addPostCategory(PostCategory category) {
        this.postCategories.add(category);
        if (category.getBoard() != this) {
            category.assignBoard(this);
        }
    }

    /** 게시판 생성 시 필수적인 기본 카테고리(공지사항, 자유게시판)를 설정합니다. */
    public void setDefaultCategories() {
        this.addPostCategory(PostCategory.createNotice(this));
        this.addPostCategory(PostCategory.createGeneral(this));
    }
}