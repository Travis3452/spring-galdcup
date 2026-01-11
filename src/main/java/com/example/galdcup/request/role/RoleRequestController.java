package com.example.galdcup.request.role;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.request.role.dto.CreateRoleRequest;
import com.example.galdcup.request.role.dto.RoleRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-changes")
@RequiredArgsConstructor
public class RoleRequestController {

    private final RoleRequestService roleRequestService;

    /** USER → MANAGER 권한 요청 */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RoleRequestDto> requestRoleChange(
            @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        RoleRequestDto roleRequestDto = roleRequestService.requestRoleChange(principal.getId(), request.requestedRole());
        return ResponseEntity.ok(roleRequestDto);
    }

    /** ADMIN → 요청 목록 조회 */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleRequestDto>> getPendingRequests() {
        List<RoleRequestDto> requests = roleRequestService.getPendingRequests();
        return ResponseEntity.ok(requests);
    }

    /** ADMIN → 요청 승인 */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveRequest(@PathVariable Long requestId) {
        roleRequestService.approveRequest(requestId);
        return ResponseEntity.ok().build();
    }

    /** ADMIN → 요청 거절 */
    @PostMapping("/{requestId}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> denyRequest(@PathVariable Long requestId) {
        roleRequestService.denyRequest(requestId);
        return ResponseEntity.ok().build();
    }
}