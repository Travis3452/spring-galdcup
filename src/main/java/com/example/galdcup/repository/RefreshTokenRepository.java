package com.example.galdcup.repository;

import com.example.galdcup.entity.RefreshToken;
import com.example.galdcup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserAndToken(User user, String token);
    void deleteByUser(User user);
}