package com.example.galdcup.service;

import com.example.galdcup.dto.user.UserDto;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.UserRepository;
import com.example.galdcup.security.AES256Encryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AES256Encryptor encryptor;

    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id)
                .map(this::decryptToDto);
    }

    public Optional<UserDto> findByOauthId(String oauthId) {
        String encryptedOauthId = encryptor.encrypt(oauthId);
        return userRepository.findByOauthId(encryptedOauthId)
                .map(this::decryptToDto);
    }

    @Transactional
    public UserDto create(User user) {
        encryptSensitiveFields(user);
        User saved = userRepository.save(user);
        return decryptToDto(saved);
    }

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

    @Transactional
    public void delete(Long id, Long currentUserId) {
        if (!id.equals(currentUserId)) {
            throw new IllegalArgumentException("본인 계정만 삭제할 수 있습니다.");
        }
        userRepository.deleteById(id);
    }

    private void encryptSensitiveFields(User user) {
        if (user.getEmail() != null) {
            user.setEmail(encryptor.encrypt(user.getEmail()));
        }
        if (user.getOauthId() != null) {
            user.setOauthId(encryptor.encrypt(user.getOauthId()));
        }
    }

    private UserDto decryptToDto(User user) {
        String decryptedEmail = user.getEmail() != null ? encryptor.decrypt(user.getEmail()) : null;

        return new UserDto(
                user.getId(),
                decryptedEmail,
                user.getNickname(),
                user.getRole().name()
        );
    }
}