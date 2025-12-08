package com.example.galdcup.service;

import com.example.galdcup.entity.User;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByOauthId(String oauthId) {
        return userRepository.findByOauthId(oauthId);
    }

    @Transactional
    public User create(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long id, String email, String nickname) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + id));

        if (email != null) user.setEmail(email);
        if (nickname != null) user.setNickname(nickname);

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User oauthSignIn(String oauthId, String email, String nickname) {
        return userRepository.findByOauthId(oauthId)
                .orElseGet(() -> create(User.builder()
                        .oauthId(oauthId)
                        .email(email)
                        .nickname(nickname)
                        .role(User.Role.USER)
                        .build()));
    }
}