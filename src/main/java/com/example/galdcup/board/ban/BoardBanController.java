package com.example.galdcup.board.ban;

import com.example.galdcup.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/{boardId}/bans")
@PreAuthorize("isAuthenticated()")
public class BoardBanController {

    private final BoardBanService service;

    @GetMapping
    public Page<BanData.View> list(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page
    ) {
        return service.listBans(boardId, user.getId(), PageRequest.of(Math.max(0, page), 20));
    }

    @PostMapping
    public BanData.View create(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody BanData.Request request
    ) {
        return service.ban(boardId, user.getId(), request);
    }

    @DeleteMapping("/{banId}")
    public void revoke(
            @PathVariable Long boardId,
            @PathVariable Long banId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        service.revokeBan(boardId, user.getId(), banId);
    }
}
