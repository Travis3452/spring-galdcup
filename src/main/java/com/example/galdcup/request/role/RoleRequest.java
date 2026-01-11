package com.example.galdcup.request.role;

import com.example.galdcup.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_changes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id")
    private User applicant;

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
