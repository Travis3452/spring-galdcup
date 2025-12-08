package com.example.galdcup.repository;

import com.example.galdcup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOauthId(String oauthId);

    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);
}