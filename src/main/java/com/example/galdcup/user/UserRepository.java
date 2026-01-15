package com.example.galdcup.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByHashOauthId(String hashOauthId);
    Page<User> findByNicknameContaining(String keyword, Pageable pageable);
    boolean existsByNickname(String nickname);

    Optional<User> findByNickname(String subManagerNickname);
}