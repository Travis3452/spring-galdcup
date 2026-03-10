package com.example.galdcup.user.validator;

import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserRepository userRepository;

    /**
     * 유저 존재 여부 확인 및 반환(id 검색)
     */
    public User findByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));
    }

    /**
     * 유저 존재 여부 확인 및 반환(nickname 검색)
     */
    public User findByNicknameOrThrow(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. Nickname: " + nickname));
    }
}
