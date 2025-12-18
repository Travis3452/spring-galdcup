package com.example.galdcup.service;

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

    public Optional<User> findById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        userOpt.ifPresent(this::decryptSensitiveFields);
        return userOpt;
    }

    public Optional<User> findByOauthId(String oauthId) {
        String encryptedOauthId = encryptor.encrypt(oauthId);
        Optional<User> userOpt = userRepository.findByOauthId(encryptedOauthId);
        userOpt.ifPresent(this::decryptSensitiveFields);
        return userOpt;
    }

    @Transactional
    public User create(User user) {
        encryptSensitiveFields(user);
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long id, String email, String nickname) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + id));

        if (email != null) user.setEmail(encryptor.encrypt(email));
        if (nickname != null) user.setNickname(nickname);

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User oauthSignIn(String oauthId, String email, String nickname) {
        String encryptedOauthId = encryptor.encrypt(oauthId);
        return userRepository.findByOauthId(encryptedOauthId)
                .orElseGet(() -> create(User.builder()
                        .oauthId(encryptedOauthId)
                        .email(encryptor.encrypt(email))
                        .nickname(nickname)
                        .role(User.Role.USER)
                        .build()));
    }

    private void encryptSensitiveFields(User user) {
        if (user.getEmail() != null) {
            user.setEmail(encryptor.encrypt(user.getEmail()));
        }
        if (user.getOauthId() != null) {
            user.setOauthId(encryptor.encrypt(user.getOauthId()));
        }
    }

    private void decryptSensitiveFields(User user) {
        if (user.getEmail() != null) {
            user.setEmail(encryptor.decrypt(user.getEmail()));
        }
        if (user.getOauthId() != null) {
            user.setOauthId(encryptor.decrypt(user.getOauthId()));
        }
    }
}