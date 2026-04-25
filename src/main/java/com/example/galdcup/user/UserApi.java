package com.example.galdcup.user;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.user.request.UserRequest;
import com.example.galdcup.user.response.RoleChangeRequestDto;
import com.example.galdcup.user.response.UserDetailDto;
import com.example.galdcup.user.response.UserDto;
import com.example.galdcup.user.role.RoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User", description = "사용자 프로필 관리 및 권한 승인 API")
public interface UserApi {

    @Operation(summary = "특정 사용자 조회", description = "ID로 사용자의 기본 정보를 조회합니다.")
    ResponseEntity<UserDto> getUser(@Parameter(description = "사용자 ID") @PathVariable Long id);

    @Operation(summary = "닉네임 검색", description = "키워드를 포함하는 닉네임의 사용자 목록을 검색합니다.")
    ResponseEntity<Page<UserDto>> getUserByNickname(
            @Parameter(description = "닉네임 키워드") @PathVariable String nickname,
            Pageable pageable);

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 상세 프로필 정보를 조회합니다.")
    ResponseEntity<UserDetailDto> me(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "프로필 수정", description = "로그인한 사용자의 닉네임을 변경합니다.")
    ResponseEntity<UserDetailDto> updateUser(
            @Parameter(description = "사용자 ID") @PathVariable Long id,
            @RequestBody UserRequest.UpdateNickname request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 삭제하고 모든 권한을 회수합니다.")
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "사용자 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "나의 권한 신청 내역 조회", description = "본인의 권한 신청 내역을 조회합니다.")
    ResponseEntity<Page<RoleChangeRequestDto>> getMyRoleRequests(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            Pageable pageable);

    @Operation(summary = "권한 변경 신청", description = "USER에서 MANAGER로 권한 변경을 신청합니다.")
    ResponseEntity<Void> requestRole(
            @RequestBody UserRequest.RoleChange request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal);

    @Operation(summary = "권한 신청 목록 조회 (관리자)", description = "시스템 관리자가 유저들의 권한 변경 신청 목록을 상태별로 조회합니다.")
    ResponseEntity<Page<RoleChangeRequestDto>> getRoleRequests(
            @Parameter(description = "신청 상태 (PENDING, APPROVED, DENIED)") @RequestParam(defaultValue = "PENDING") RoleRequest.Status status,
            Pageable pageable);

    @Operation(summary = "권한 신청 승인 (관리자)", description = "특정 유저의 권한 변경 신청을 승인합니다.")
    ResponseEntity<Void> approveRole(@Parameter(description = "신청 요청 ID") @PathVariable Long requestId);

    @Operation(summary = "권한 신청 거절 (관리자)", description = "특정 유저의 권한 변경 신청을 거절합니다.")
    ResponseEntity<Void> denyRole(@Parameter(description = "신청 요청 ID") @PathVariable Long requestId);
}