package com.example.galdcup.board.postCategory.domain;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.post.domain.Post;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 분류 엔티티
 */
@Entity
@Table(
        name="post_categories",
        indexes = {
                @Index(name = "idx_category_board_sort", columnList = "board_id, sortOrder")
        }
)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @OneToMany(mappedBy = "postCategory")
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    public void assignBoard(Board board) {
        this.board = board;
    }

    public void changeSortOrder(int newSortOrder) {
        this.sortOrder = newSortOrder;
    }

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
                .sortOrder(0)
                .board(board)
                .build();
    }

    public static PostCategory createNotice(Board board) {
        return PostCategory.builder()
                .name("공지사항")
                .type(CategoryType.NOTICE)
                .sortOrder(1)
                .board(board)
                .build();
    }

    public static PostCategory createCustom(Board board, String name, int sortOrder) {
        return PostCategory.builder()
                .name(name)
                .type(CategoryType.CUSTOM)
                .sortOrder(sortOrder)
                .board(board)
                .build();
    }

    /**
     * 카테고리 정보 수정 (이름, 순서)
     */
    public void update(String newName, Integer newSortOrder) {
        if (newName != null && !this.name.equals(newName)) {
            if (this.type != CategoryType.CUSTOM) {
                throw new IllegalArgumentException("기본 카테고리의 이름은 변경할 수 없습니다.");
            }
            this.name = newName;
        }

        if (newSortOrder != null) {
            this.sortOrder = newSortOrder;
        }
    }
}