package com.example.galdcup.service;

import com.example.galdcup.entity.RoleChange;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.RoleChangeRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleChangeService {

    private final RoleChangeRepository roleChangeRepository;
    private final UserRepository userRepository;

    /** USER → MANAGER 요청 생성 */
    @Transactional
    public RoleChange requestRoleChange(Long userId, User.Role requestedRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        RoleChange roleChange = RoleChange.builder()
                .user(user)
                .requestedRole(requestedRole)
                .status(RoleChange.Status.WAITING)
                .build();

        return roleChangeRepository.save(roleChange);
    }

    /** ADMIN → 요청 목록 조회 */
    @Transactional(readOnly = true)
    public List<RoleChange> getPendingRequests() {
        return roleChangeRepository.findByStatus(RoleChange.Status.WAITING);
    }

    /** ADMIN → 요청 승인 */
    @Transactional
    public void approveRequest(Long requestId) {
        RoleChange roleChange = roleChangeRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

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

        roleChange.setStatus(RoleChange.Status.DENIED);
        roleChangeRepository.save(roleChange);
    }
}