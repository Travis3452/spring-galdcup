package com.example.galdcup.user;

import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.user.dto.UpdateUserRequest;
import com.example.galdcup.user.dto.UserDetailDto;
import com.example.galdcup.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 특정 사용자 조회(id) */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        UserDto userDto = userService.findById(id);

        return ResponseEntity.ok(userDto);
    }

    /** 닉네임 포함 검색 (페이징) */
    @GetMapping("/nickname/{nickname}")
    public ResponseEntity<Page<UserDto>> getUserByNickname(@PathVariable String nickname,
                                                           Pageable pageable) {
        Page<UserDto> users = userService.findByNicknameContaining(nickname, pageable);
        return ResponseEntity.ok(users);
    }

    /** 프로필 수정 (본인만 가능) */
    @PutMapping("/{id}")
    public ResponseEntity<UserDetailDto> updateUser(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateUserRequest request,
                                                    @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        UserDetailDto updated = userService.updateProfile(id, request.nickname(), principal.getId());
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
    public ResponseEntity<UserDetailDto> me(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        UserDetailDto userDetailDto = userService.findUserDetailById(principal.getId());

        return ResponseEntity.ok(userDetailDto);
    }
}