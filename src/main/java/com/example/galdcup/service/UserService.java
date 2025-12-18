package com.example.galdcup.service;

import com.example.galdcup.dto.user.UserDto;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.UserRepository;
import com.example.galdcup.security.AES256Encryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AES256Encryptor encryptor;

    public UserService(UserRepository userRepository,
                       @Value("${aes256.key}") String base64Key) {
        this.userRepository = userRepository;
        this.encryptor = AES256Encryptor.fromBase64Key(base64Key);
    }

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
    public UserDto updateProfile(Long id, String email, String nickname) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + id));

        if (email != null) user.setEmail(encryptor.encrypt(email));
        if (nickname != null) user.setNickname(nickname);

        User updated = userRepository.save(user);
        return decryptToDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public UserDto oauthSignIn(String oauthId, String email, String nickname) {
        String encryptedOauthId = encryptor.encrypt(oauthId);

        User user = userRepository.findByOauthId(encryptedOauthId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .oauthId(encryptedOauthId)
                        .email(encryptor.encrypt(email))
                        .nickname(nickname)
                        .role(User.Role.USER)
                        .build()));

        return decryptToDto(user);
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