package com.example.galdcup.rolechange;

import com.example.galdcup.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_changes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private User.Role requestedRole;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        WAITING,
        ACCEPTED,
        DENIED
    }
}
