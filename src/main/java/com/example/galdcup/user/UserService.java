package com.example.galdcup.user;

import com.example.galdcup.comment.CommentRepository;
import com.example.galdcup.comment.ReplyRepository;
import com.example.galdcup.post.PostRepository;
import com.example.galdcup.common.security.AES256Encryptor;
import com.example.galdcup.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;
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

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        User deletedUser = userRepository.findById(5L)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        postRepository.updateAuthorToDeletedUser(user.getId(), deletedUser);
        commentRepository.updateAuthorToDeletedUser(user.getId(), deletedUser);
        replyRepository.updateAuthorToDeletedUser(user.getId(), deletedUser);

        userRepository.delete(user);
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