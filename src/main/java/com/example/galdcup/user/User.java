package com.example.galdcup.user;

import com.example.galdcup.board.domain.BoardManagerRequest;
import com.example.galdcup.user.role.RoleRequest;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
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
        USER, MANAGER, ADMIN
    }

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<RoleRequest> roleRequests = new ArrayList<>();

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<BoardManagerRequest> boardManagerRequests = new ArrayList<>();

    public static User signup(String email, String oauthId, String nickname) {
        return User.builder()
                .email(email)
                .oauthId(oauthId)
                .nickname(nickname)
                .role(Role.USER)
                .build();
    }

    /**
     * 닉네임 변경
     */
    public void changeNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }
        this.nickname = newNickname;
    }

    /**
     * 권한(Role) 변경
     */
    public void upgradeRole(Role newRole) {
        this.role = newRole;
    }
}