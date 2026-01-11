package com.example.galdcup.request.role;

import com.example.galdcup.common.security.AES256Encryptor;
import com.example.galdcup.request.role.dto.RoleRequestDto;
import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleRequestService {

    private final RoleRequestRepository roleRequestRepository;
    private final UserRepository userRepository;
    private final AES256Encryptor encryptor;

    /** USER → MANAGER 요청 생성 */
    @Transactional
    public RoleRequestDto requestRoleChange(Long applicantId, User.Role requestedRole) {
        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이미 해당 권한을 가지고 있는 경우
        if (applicant.getRole() == requestedRole) {
            throw new IllegalStateException("이미 해당 권한을 가지고 있습니다.");
        }

        // 대기 중인 요청이 있는 경우
        boolean existsPending = roleRequestRepository.existsByApplicantIdAndStatus(applicantId, RoleRequest.Status.WAITING);
        if (existsPending) {
            throw new IllegalStateException("이미 대기 중인 권한 요청이 있습니다.");
        }

        RoleRequest roleRequest = RoleRequest.builder()
                .applicant(applicant)
                .requestedRole(requestedRole)
                .status(RoleRequest.Status.WAITING)
                .build();

        RoleRequest saved = roleRequestRepository.save(roleRequest);
        return toDto(saved);
    }

    /** ADMIN → 요청 목록 조회 */
    @Transactional(readOnly = true)
    public List<RoleRequestDto> getPendingRequests() {
        return roleRequestRepository.findByStatus(RoleRequest.Status.WAITING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** ADMIN → 요청 승인 */
    @Transactional
    public void approveRequest(Long requestId) {
        RoleRequest roleRequest = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

        if (roleRequest.getStatus() != RoleRequest.Status.WAITING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        roleRequest.setStatus(RoleRequest.Status.ACCEPTED);
        User applicant = roleRequest.getApplicant();
        applicant.setRole(roleRequest.getRequestedRole());

        roleRequestRepository.save(roleRequest);
        userRepository.save(applicant);
    }

    /** ADMIN → 요청 거절 */
    @Transactional
    public void denyRequest(Long requestId) {
        RoleRequest roleRequest = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

        if (roleRequest.getStatus() != RoleRequest.Status.WAITING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        roleRequest.setStatus(RoleRequest.Status.DENIED);
        roleRequestRepository.save(roleRequest);
    }

    /** 엔티티 → DTO 변환 (이메일 복호화 포함) */
    private RoleRequestDto toDto(RoleRequest roleRequest) {
        User applicant = roleRequest.getApplicant();
        String decryptedEmail = applicant.getEncryptedEmail() != null
                ? encryptor.decrypt(applicant.getEncryptedEmail())
                : null;

        return new RoleRequestDto(
                roleRequest.getId(),
                applicant.getId(),
                decryptedEmail,
                applicant.getNickname(),
                roleRequest.getRequestedRole(),
                roleRequest.getStatus()
        );
    }
}