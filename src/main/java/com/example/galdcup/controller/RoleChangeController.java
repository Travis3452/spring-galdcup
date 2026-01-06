package com.example.galdcup.controller;

import com.example.galdcup.dto.rolechange.CreateRoleChangeRequest;
import com.example.galdcup.dto.rolechange.RoleChangeDto;
import com.example.galdcup.entity.RoleChange;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.RoleChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-changes")
@RequiredArgsConstructor
public class RoleChangeController {

    private final RoleChangeService roleChangeService;

    /** USER → MANAGER 권한 요청 */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RoleChangeDto> requestRoleChange(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody CreateRoleChangeRequest request) {

        RoleChange roleChange = roleChangeService.requestRoleChange(principal.getId(), request.requestedRole());
        return ResponseEntity.ok(RoleChangeDto.from(roleChange));
    }

    /** ADMIN → 요청 목록 조회 */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleChangeDto>> getPendingRequests() {
        List<RoleChangeDto> requests = roleChangeService.getPendingRequests()
                .stream()
                .map(RoleChangeDto::from)
                .toList();
        return ResponseEntity.ok(requests);
    }

    /** ADMIN → 요청 승인 */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveRequest(@PathVariable Long requestId) {
        roleChangeService.approveRequest(requestId);
        return ResponseEntity.ok().build();
    }

    /** ADMIN → 요청 거절 */
    @PostMapping("/{requestId}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> denyRequest(@PathVariable Long requestId) {
        roleChangeService.denyRequest(requestId);
        return ResponseEntity.ok().build();
    }
}