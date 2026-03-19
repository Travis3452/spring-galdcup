package com.example.galdcup.user;

import com.example.galdcup.request.boardmanager.BoardManagerRequest;
import com.example.galdcup.user.role.RoleRequest;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String oauthId;

    @Column(unique = true, nullable = false, length = 14)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        USER,
        MANAGER,
        ADMIN
    }

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<RoleRequest> roleRequests = new ArrayList<>();

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<BoardManagerRequest> boardManagerRequests = new ArrayList<>();
}