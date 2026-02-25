package com.example.galdcup.user;

import com.example.galdcup.request.boardmanager.BoardManagerRequest;
import com.example.galdcup.request.role.RoleRequest;
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

    private String encryptedEmail;   // AES 암호화된 이메일
    private String hashEmail;        // SHA-256 해시된 이메일

    private String encryptedOauthId; // AES 암호화된 OAuth ID
    private String hashOauthId;      // SHA-256 해시된 OAuth ID

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        USER,
        MANAGER,
        ADMIN
    }

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<RoleRequest> roleRequests = new ArrayList<>();

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<BoardManagerRequest> boardManagerRequests = new ArrayList<>();
}