package com.example.galdcup.controller;

import com.example.galdcup.dto.user.*;
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
        Optional<UserDto> userOpt = userService.findById(id);
        return userOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto saved = userService.create(
                com.example.galdcup.entity.User.builder()
                        .oauthId(request.oauthId())
                        .email(request.email())
                        .nickname(request.nickname())
                        .role(com.example.galdcup.entity.User.Role.USER)
                        .build()
        );

        return ResponseEntity.created(URI.create("/api/users/" + saved.id()))
                .body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UpdateUserRequest request) {
        UserDto updated = userService.updateProfile(id, request.email(), request.nickname());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        String oauthId = principal.getName();
        String email = principal.getAttribute("email");
        String nickname = principal.getAttribute("nickname");

        UserDto user = userService.oauthSignIn(oauthId, email, nickname);
        return ResponseEntity.ok(user);
    }
}