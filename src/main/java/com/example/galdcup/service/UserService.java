package com.example.galdcup.service;

import com.example.galdcup.dto.user.UserDto;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.UserRepository;
import com.example.galdcup.security.AES256Encryptor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AES256Encryptor encryptor;

    /** ID로 사용자 조회 후 DTO 반환 */
    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id)
                .map(this::decryptToDto);
    }

    /** 사용자 프로필 수정 (본인만 가능) */
    @Transactional
    public UserDto updateProfile(Long id, String nickname, Long currentUserId) {
        if (!id.equals(currentUserId)) {
            throw new IllegalArgumentException("본인만 프로필을 수정할 수 있습니다.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + id));

        if (nickname != null) user.setNickname(nickname);

        User updated = userRepository.save(user);
        return decryptToDto(updated);
    }

    /** 사용자 삭제 (본인만 가능) */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        if (!id.equals(currentUserId)) {
            throw new IllegalArgumentException("본인 계정만 삭제할 수 있습니다.");
        }
        userRepository.deleteById(id);
    }

    /** 엔티티를 DTO로 변환 (이메일, OAuth ID 복호화 포함) */
    private UserDto decryptToDto(User user) {
        String decryptedEmail = user.getEmailEncrypted() != null
                ? encryptor.decrypt(user.getEmailEncrypted())
                : null;

        return new UserDto(
                user.getId(),
                decryptedEmail,
                user.getNickname(),
                user.getRole().name()
        );
    }
}