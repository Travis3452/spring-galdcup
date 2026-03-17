package com.example.galdcup.user;

import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.user.dto.UserDetailDto;
import com.example.galdcup.user.dto.UserDto;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final UserValidator userValidator;
    private final UserNicknameGenerator nicknameGenerator;

    /**
     * OAuth 정보를 바탕으로 유저를 조회하거나 새로 생성 (회원가입/로그인)
     */
    @Transactional
    public User getOrCreateUser(String oauthId, String email) {
        return userRepository.findByOauthId(oauthId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .oauthId(oauthId)
                        .nickname(nicknameGenerator.generate())
                        .role(User.Role.MANAGER)
                        .build()));
    }

    /**
     * ID로 사용자 조회 (간단 정보 DTO)
     */
    public UserDto findById(Long id) {
        User user = userValidator.findByIdOrThrow(id);
        return UserDto.from(user);
    }

    /**
     * ID로 사용자 상세 정보 조회
     */
    public UserDetailDto findUserDetailById(Long id) {
        User user = userValidator.findByIdOrThrow(id);
        return UserDetailDto.from(user);
    }

    /**
     * 닉네임 키워드로 사용자 검색 (페이징)
     */
    public Page<UserDto> findByNicknameContaining(String keyword, Pageable pageable) {
        return userRepository.findByNicknameContaining(keyword, pageable)
                .map(UserDto::from);
    }

    /**
     * 사용자 프로필 수정 (닉네임 변경)
     */
    @Transactional
    public UserDetailDto updateProfile(Long id, String nickname, Long currentUserId) {
        userValidator.validateOwnership(id, currentUserId);
        User user = userValidator.findByIdOrThrow(id);

        if (nickname != null && !nickname.equals(user.getNickname())) {
            userValidator.validateNicknameUniqueness(nickname);
            user.setNickname(nickname);
        }

        return UserDetailDto.from(user);
    }

    /**
     * 사용자 삭제 (회원 탈퇴)
     */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        userValidator.validateOwnership(id, currentUserId);
        User user = userValidator.findByIdOrThrow(id);

        boardRepository.removeBoardManagerByUserId(user.getId());

        userRepository.delete(user);
    }
}