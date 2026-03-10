package com.example.galdcup.postCategory;

import com.example.galdcup.board.Board;
import com.example.galdcup.post.Post;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name="post_categories",
        indexes = {
                @Index(name = "idx_category_board_id", columnList = "board_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PostCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @OneToMany(mappedBy = "postCategory")
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    public enum CategoryType {
        GENERAL,
        NOTICE,
        CUSTOM
    }

    public boolean isRemovable() {
        return this.type == CategoryType.CUSTOM;
    }

    public static PostCategory createGeneral(Board board) {
        return PostCategory.builder()
                .name("일반")
                .type(CategoryType.GENERAL)
                .board(board)
                .build();
    }

    public static PostCategory createNotice(Board board) {
        return PostCategory.builder()
                .name("공지사항")
                .type(CategoryType.NOTICE)
                .board(board)
                .build();
    }

    public static PostCategory createCustom(Board board, String name) {
        return PostCategory.builder()
                .name(name)
                .type(CategoryType.CUSTOM)
                .board(board)
                .build();
    }
}