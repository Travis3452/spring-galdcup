package com.example.galdcup.user;

import com.example.galdcup.common.rateLimit.RateLimit;
import com.example.galdcup.common.rateLimit.RateLimitType;
import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.user.request.UserRequest;
import com.example.galdcup.user.response.RoleChangeRequestDto;
import com.example.galdcup.user.response.UserDetailDto;
import com.example.galdcup.user.response.UserDto;
import com.example.galdcup.user.role.RoleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    /**
     * 특정 사용자 조회 (ID 기반)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * 닉네임 키워드 검색 (페이징)
     */
    @GetMapping("/nickname/{nickname}")
    public ResponseEntity<Page<UserDto>> getUserByNickname(
            @PathVariable String nickname,
            Pageable pageable) {
        return ResponseEntity.ok(userService.findByNicknameContaining(nickname, pageable));
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDetailDto> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.findUserDetailById(principal.getId()));
    }

    /**
     * 프로필 수정 (닉네임 변경) - 본인만 가능
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<UserDetailDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest.UpdateNickname request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        return ResponseEntity.ok(userService.updateProfile(id, request.nickname(), principal.getId()));
    }

    /**
     * 사용자 삭제 (회원 탈퇴) - 본인만 가능
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        userService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 권한 관리 (Role Management)
    // ==========================================

    /**
     * [내 정보] 나의 권한 신청 내역 조회
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/role-requests")
    public ResponseEntity<Page<RoleChangeRequestDto>> getMyRoleRequests(
            @AuthenticationPrincipal CustomUserDetails principal,
            Pageable pageable) {

        return ResponseEntity.ok(userService.findMyRoleRequests(principal.getId(), pageable));
    }

    /**
     * 권한 변경 신청 (USER -> MANAGER 등)
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/role-requests")
    public ResponseEntity<Void> requestRole(
            @Valid @RequestBody UserRequest.RoleChange request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        userService.requestRoleChange(principal.getId(), request.requestedRole());
        return ResponseEntity.accepted().build();
    }

    /**
     * [관리자 전용] 권한 신청 목록 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/role-requests")
    public ResponseEntity<Page<RoleChangeRequestDto>> getRoleRequests(
            @RequestParam(defaultValue = "PENDING") RoleRequest.Status status,
            Pageable pageable) {

        return ResponseEntity.ok(userService.findRoleRequestsByStatus(status, pageable));
    }

    /**
     * [관리자 전용] 권한 신청 승인
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/role-requests/{requestId}/approve")
    public ResponseEntity<Void> approveRole(@PathVariable Long requestId) {
        userService.approveRoleChange(requestId);
        return ResponseEntity.ok().build();
    }

    /**
     * [관리자 전용] 권한 신청 거절
     */
    @RateLimit(type = RateLimitType.INTERNAL)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/role-requests/{requestId}/deny")
    public ResponseEntity<Void> denyRole(@PathVariable Long requestId) {
        userService.denyRoleChange(requestId);
        return ResponseEntity.noContent().build();
    }
}