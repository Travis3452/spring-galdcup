package com.example.galdcup.user.role;

import com.example.galdcup.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_requests")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
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

    public static RoleRequest create(User applicant, User.Role requestedRole) {
        return RoleRequest.builder()
                .applicant(applicant)
                .requestedRole(requestedRole)
                .status(Status.PENDING)
                .build();
    }

    public void approve() {
        this.status = Status.APPROVED;
        this.applicant.upgradeRole(this.requestedRole);
    }

    public void deny() {
        this.status = Status.DENIED;
    }
}
