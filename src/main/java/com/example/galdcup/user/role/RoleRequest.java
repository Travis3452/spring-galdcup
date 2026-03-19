package com.example.galdcup.user.role;

import com.example.galdcup.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_requests")
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
        PENDING,
        APPROVED,
        DENIED
    }
}
