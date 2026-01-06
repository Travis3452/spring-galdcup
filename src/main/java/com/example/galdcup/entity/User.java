package com.example.galdcup.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String emailEncrypted;   // AES 암호화된 이메일
    private String emailHash;        // SHA-256 해시된 이메일

    private String oauthIdEncrypted; // AES 암호화된 OAuth ID
    private String oauthIdHash;      // SHA-256 해시된 OAuth ID

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        USER,
        MANAGER,
        ADMIN
    }
}