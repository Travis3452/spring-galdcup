package com.example.galdcup.controller;

import com.example.galdcup.dto.user.*;
import com.example.galdcup.entity.User;
import com.example.galdcup.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        Optional<User> userOpt = userService.findById(id);
        return userOpt.map(u -> ResponseEntity.ok(UserDto.from(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        User saved = userService.create(User.builder()
                .oauthId(request.oauthId())
                .email(request.email())
                .nickname(request.nickname())
                .role(User.Role.USER)
                .build());

        return ResponseEntity.created(URI.create("/api/users/" + saved.getId()))
                .body(UserDto.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UpdateUserRequest request) {
        User updated = userService.updateProfile(id, request.email(), request.nickname());
        return ResponseEntity.ok(UserDto.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth/signin")
    public ResponseEntity<UserDto> oauthSignIn(@Valid @RequestBody OauthSignInRequest request) {
        User user = userService.oauthSignIn(request.oauthId(), request.email(), request.nickname());
        return ResponseEntity.ok(UserDto.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        String oauthId = principal.getName();
        String email = principal.getAttribute("email");
        String nickname = principal.getAttribute("nickname");

        User user = userService.oauthSignIn(oauthId, email, nickname);
        return ResponseEntity.ok(UserDto.from(user));
    }
}