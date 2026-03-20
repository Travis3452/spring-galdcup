package com.example.galdcup.board.domain;

import com.example.galdcup.post.domain.Post;
import com.example.galdcup.postCategory.PostCategory;
import com.example.galdcup.voteSession.VoteSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Board {

    public enum Status { OPEN, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(length = 500)
    private String description;

    @OneToOne(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
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
        this.status = Status.OPEN;
    }

    public void addPostCategory(PostCategory category) {
        this.postCategories.add(category);
        if (category.getBoard() != this) {
            category.setBoard(this);
        }
    }

    public void setDefaultCategories() {
        this.addPostCategory(PostCategory.createNotice(this));
        this.addPostCategory(PostCategory.createGeneral(this));
    }

    public void closeBoard() {
        this.status = Status.CLOSED;
    }
}