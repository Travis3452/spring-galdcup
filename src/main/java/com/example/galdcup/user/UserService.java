package com.example.galdcup.user;

import com.example.galdcup.board.domain.BoardRepository;
import com.example.galdcup.user.dto.RoleChangeRequestDto;
import com.example.galdcup.user.dto.UserDetailDto;
import com.example.galdcup.user.dto.UserDto;
import com.example.galdcup.user.role.RoleRequest;
import com.example.galdcup.user.role.RoleRequestRepository;
import com.example.galdcup.user.validator.RoleRequestValidator;
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
    private final RoleRequestRepository roleRequestRepository;
    private final UserValidator userValidator;
    private final RoleRequestValidator roleRequestValidator;
    private final UserNicknameGenerator nicknameGenerator;

    /**
     * OAuth 정보를 바탕으로 유저를 조회하거나 새로 생성 (회원가입/로그인)
     */
    @Transactional
    public User getOrCreateUser(String oauthId, String email) {
        return userRepository.findByOauthId(oauthId)
                .orElseGet(() -> {
                    String nickname = nicknameGenerator.generate();
                    User newUser = User.signup(email, oauthId, nickname);
                    return userRepository.save(newUser);
                });
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
            user.changeNickname(nickname);
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

    // ==========================================
    // 권한 신청 관리 (Role Management)
    // ==========================================

    /**
     * 나의 권한 신청 내역 조회
     */
    public Page<RoleChangeRequestDto> findMyRoleRequests(Long userId, Pageable pageable) {
        return roleRequestRepository.findAllByApplicantId(userId, pageable)
                .map(RoleChangeRequestDto::from);
    }

    /**
     * 권한 변경 신청 접수
     */
    @Transactional
    public void requestRoleChange(Long userId, String requestedRole) {
        User user = userValidator.findByIdOrThrow(userId);
        User.Role targetRole = User.Role.valueOf(requestedRole);

        roleRequestValidator.validateRequestAvailability(user, targetRole);

        RoleRequest roleRequest = RoleRequest.create(user, targetRole);

        roleRequestRepository.save(roleRequest);
    }

    /**
     * [관리자] 대기 상태의 신청 목록 조회
     */
    public Page<RoleChangeRequestDto> findRoleRequestsByStatus(RoleRequest.Status status, Pageable pageable) {
        return roleRequestRepository.findAllByStatus(status, pageable)
                .map(RoleChangeRequestDto::from);
    }

    /**
     * [관리자] 권한 신청 승인
     */
    @Transactional
    public void approveRoleChange(Long requestId) {
        RoleRequest roleRequest = roleRequestValidator.findByIdOrThrow(requestId);
        roleRequestValidator.validatePendingStatus(roleRequest);

        roleRequest.approve();
    }

    /**
     * [관리자] 권한 신청 거절
     */
    @Transactional
    public void denyRoleChange(Long requestId) {
        RoleRequest roleRequest = roleRequestValidator.findByIdOrThrow(requestId);
        roleRequestValidator.validatePendingStatus(roleRequest);

        roleRequest.deny();
    }
}