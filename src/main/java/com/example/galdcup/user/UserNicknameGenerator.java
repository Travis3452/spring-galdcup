package com.example.galdcup.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserNicknameGenerator {

    private final UserRepository userRepository;

    public String generate() {
        String nickname;

        do {
            nickname = "user-" + UUID.randomUUID().toString().substring(0, 8);

        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }
}