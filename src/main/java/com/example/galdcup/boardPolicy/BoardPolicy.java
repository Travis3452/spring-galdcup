package com.example.galdcup.boardPolicy;

import com.example.galdcup.board.Board;
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
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_manager_id", nullable = false)
    private User boardManager;

    @ManyToMany
    @JoinTable(
            name = "board_policy_sub_managers",
            joinColumns = @JoinColumn(name = "policy_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> subManagers = new ArrayList<>();

    private long likeThreshold;
}