package com.example.galdcup.controller;

import com.example.galdcup.dto.user.UpdateUserRequest;
import com.example.galdcup.dto.user.UserDto;
import com.example.galdcup.security.CustomUserDetails;
import com.example.galdcup.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 특정 사용자 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        Optional<UserDto> userOpt = userService.findById(id);
        return userOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 프로필 수정 (본인만 가능) */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UpdateUserRequest request,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        UserDto updated = userService.updateProfile(id, request.nickname(), principal.getId());
        return ResponseEntity.ok(updated);
    }

    /** 사용자 삭제 (본인만 가능) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        userService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    /** 현재 로그인한 사용자 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        return userService.findByOauthId(principal.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}