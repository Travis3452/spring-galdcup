package com.example.galdcup.board.domain;

import com.example.galdcup.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "board_policies",
        indexes = {
                @Index(name = "idx_board_policy_board_id", columnList = "board_id")
        }
)
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_manager_id")
    private User boardManager;

    @ManyToMany
    @JoinTable(
            name = "board_policy_sub_managers",
            joinColumns = @JoinColumn(name = "policy_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private List<User> subManagers = new ArrayList<>();

    private long likeThreshold;

    public static BoardPolicy create(Board board, User manager) {
        return BoardPolicy.builder()
                .board(board)
                .boardManager(manager)
                .likeThreshold(20)
                .build();
    }

    public void delegateMainManager(User newManager) {
        if (this.boardManager != null && this.boardManager.equals(newManager)) {
            throw new IllegalArgumentException("자기 자신에게는 권한을 위임할 수 없습니다.");
        }

        this.subManagers.remove(newManager);
        this.boardManager = newManager;
    }

    public void addSubManager(User user) {
        if (this.subManagers.contains(user)) {
            throw new IllegalArgumentException("이미 등록된 서브 매니저입니다.");
        }
        this.subManagers.add(user);
    }

    public boolean isMainManager(User user) {
        if (user == null || this.boardManager == null) return false;
        return this.boardManager.equals(user);
    }

    public boolean isSubManager(User user) {
        return this.subManagers.contains(user);
    }

    public boolean isAnyManager(User user) {
        return isMainManager(user) || isSubManager(user);
    }

    public void removeSubManager(User user) {
        this.subManagers.remove(user);
    }

    public void updateLikeThreshold(long newThreshold) {
        this.likeThreshold = newThreshold;
    }
}