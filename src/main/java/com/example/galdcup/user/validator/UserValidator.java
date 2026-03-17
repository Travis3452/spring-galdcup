package com.example.galdcup.user.validator;

import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {
    private final UserRepository userRepository;

    public User findByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));
    }

    public User findByNicknameOrThrow(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. Nickname: " + nickname));
    }

    /**
     * 닉네임 중복 여부 검증
     */
    public void validateNicknameUniqueness(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다. : " + nickname);
        }
    }

    /**
     * 본인 확인 검증
     */
    public void validateOwnership(Long targetId, Long currentUserId) {
        if (!targetId.equals(currentUserId)) {
            throw new IllegalArgumentException("해당 요청에 대한 권한이 없습니다.");
        }
    }
}