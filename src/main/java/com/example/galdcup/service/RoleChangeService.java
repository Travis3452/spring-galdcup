package com.example.galdcup.service;

import com.example.galdcup.dto.rolechange.RoleChangeDto;
import com.example.galdcup.entity.RoleChange;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.RoleChangeRepository;
import com.example.galdcup.repository.UserRepository;
import com.example.galdcup.security.AES256Encryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleChangeService {

    private final RoleChangeRepository roleChangeRepository;
    private final UserRepository userRepository;
    private final AES256Encryptor encryptor;

    /** USER → MANAGER 요청 생성 */
    @Transactional
    public RoleChangeDto requestRoleChange(Long userId, User.Role requestedRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이미 해당 권한을 가지고 있는 경우
        if (user.getRole() == requestedRole) {
            throw new IllegalStateException("이미 해당 권한을 가지고 있습니다.");
        }

        // 대기 중인 요청이 있는 경우
        boolean existsPending = roleChangeRepository.existsByUserIdAndStatus(userId, RoleChange.Status.WAITING);
        if (existsPending) {
            throw new IllegalStateException("이미 대기 중인 권한 요청이 있습니다.");
        }

        RoleChange roleChange = RoleChange.builder()
                .user(user)
                .requestedRole(requestedRole)
                .status(RoleChange.Status.WAITING)
                .build();

        RoleChange saved = roleChangeRepository.save(roleChange);
        return toDto(saved);
    }

    /** ADMIN → 요청 목록 조회 */
    @Transactional(readOnly = true)
    public List<RoleChangeDto> getPendingRequests() {
        return roleChangeRepository.findByStatus(RoleChange.Status.WAITING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** ADMIN → 요청 승인 */
    @Transactional
    public void approveRequest(Long requestId) {
        RoleChange roleChange = roleChangeRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

        if (roleChange.getStatus() != RoleChange.Status.WAITING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        roleChange.setStatus(RoleChange.Status.ACCEPTED);
        User user = roleChange.getUser();
        user.setRole(roleChange.getRequestedRole());

        roleChangeRepository.save(roleChange);
        userRepository.save(user);
    }

    /** ADMIN → 요청 거절 */
    @Transactional
    public void denyRequest(Long requestId) {
        RoleChange roleChange = roleChangeRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

        if (roleChange.getStatus() != RoleChange.Status.WAITING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        roleChange.setStatus(RoleChange.Status.DENIED);
        roleChangeRepository.save(roleChange);
    }

    /** 엔티티 → DTO 변환 (이메일 복호화 포함) */
    private RoleChangeDto toDto(RoleChange roleChange) {
        User user = roleChange.getUser();
        String decryptedEmail = user.getEmailEncrypted() != null
                ? encryptor.decrypt(user.getEmailEncrypted())
                : null;

        return new RoleChangeDto(
                roleChange.getId(),
                user.getId(),
                decryptedEmail,
                user.getNickname(),
                roleChange.getRequestedRole(),
                roleChange.getStatus()
        );
    }
}