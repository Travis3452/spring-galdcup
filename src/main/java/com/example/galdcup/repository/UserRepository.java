package com.example.galdcup.repository;

import com.example.galdcup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    /** OAuth ID 해시로 사용자 조회 */
    Optional<User> findByOauthIdHash(String oauthIdHash);
}