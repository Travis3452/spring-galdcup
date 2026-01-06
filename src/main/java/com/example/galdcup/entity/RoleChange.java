package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_changes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 요청한 Role
    @Enumerated(EnumType.STRING)
    private User.Role requestedRole;

    // 요청 상태
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        WAITING,
        ACCEPTED,
        DENIED
    }
}
