package com.example.galdcup.user.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 신규 가입 사용자를 위한 고유 임시 닉네임 자동 생성 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class UserNicknameGenerator {

    private final UserRepository userRepository;

    /**
     * UUID 기반의 랜덤 닉네임 생성
     */
    public String generate() {
        String nickname;

        do {
            nickname = "user-" + UUID.randomUUID().toString().substring(0, 8);

        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }
}