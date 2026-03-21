package com.example.galdcup.user.validator;

import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.role.RoleRequest;
import com.example.galdcup.user.role.RoleRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleRequestValidator {

    private final RoleRequestRepository roleRequestRepository;

    /**
     * ID로 신청 내역 조회 또는 예외 발생
     */
    public RoleRequest findByIdOrThrow(Long id) {
        return roleRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청 내역입니다."));
    }

    /**
     * 신청 가능 여부 검증 (동일 권한 및 중복 신청 체크)
     */
    public void validateRequestAvailability(User user, User.Role targetRole) {
        if (user.getRole() == targetRole) {
            throw new IllegalStateException("이미 해당 권한을 보유하고 있습니다.");
        }

        if (roleRequestRepository.existsByApplicantIdAndStatus(user.getId(), RoleRequest.Status.PENDING)) {
            throw new IllegalStateException("이미 대기 중인 권한 신청이 있습니다.");
        }
    }

    /**
     * 처리 가능한 상태(PENDING)인지 검증
     */
    public void validatePendingStatus(RoleRequest roleRequest) {
        if (roleRequest.getStatus() != RoleRequest.Status.PENDING) {
            throw new IllegalStateException("이미 처리된 신청 건입니다.");
        }
    }
}