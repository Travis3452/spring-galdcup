package com.example.galdcup.board.ban;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.board.domain.BoardRepository;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardBanService {

    private final BoardValidator boardValidator;
    private final UserValidator userValidator;
    private final BoardRepository boardRepository;
    private final BoardBanRepository bans;

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.example.galdcup.board.ban.BanData.View> listBans(
            Long boardId, Long actor, org.springframework.data.domain.Pageable page
    ) {
        boardValidator.requireBoardManagementAccess(boardId, actor);
        return bans.findByBoardIdOrderByCreatedAtDescIdDesc(boardId, page)
                .map(com.example.galdcup.board.ban.BanData.View::from);
    }

    @Transactional
    public com.example.galdcup.board.ban.BanData.View ban(
            Long boardId, Long actor, com.example.galdcup.board.ban.BanData.Request request
    ) {
        boardRepository.lockForModeration(boardId).orElseThrow();
        Board board = boardValidator.requireBoardManagementAccess(boardId, actor);
        User target = userValidator.findByIdOrThrow(request.userId());

        if (board.getBoardPolicy().isAnyManager(target.getId()))
            throw new IllegalArgumentException("관리자는 해임 후 차단할 수 있습니다.");

        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        if (!request.permanent() && request.expiresAt() == null)
            throw new IllegalArgumentException("차단 종료 시각을 지정해 주세요.");
        if (request.permanent() && request.expiresAt() != null)
            throw new IllegalArgumentException("영구 차단에는 종료 시각을 지정하지 않습니다.");
        if (bans.countActive(boardId, target.getId(), now) > 0)
            throw new IllegalStateException("이미 차단된 사용자입니다. 기존 차단을 해제한 후 다시 설정해 주세요.");

        return com.example.galdcup.board.ban.BanData.View.from(
                bans.save(com.example.galdcup.board.ban.BoardBan.create(
                        board, target.getId(), target.getNickname(), request.reason().trim(),
                        request.expiresAt(), actor, now
                ))
        );
    }

    @Transactional
    public void revokeBan(Long boardId, Long actor, Long banId) {
        boardRepository.lockForModeration(boardId).orElseThrow();
        boardValidator.requireBoardManagementAccess(boardId, actor);
        var ban = bans.findById(banId)
                .orElseThrow(() -> new IllegalArgumentException("차단 기록이 없습니다."));

        if (!ban.getBoard().getId().equals(boardId))
            throw new org.springframework.security.access.AccessDeniedException("다른 게시판의 차단 기록입니다.");

        ban.revoke(actor, java.time.OffsetDateTime.now());
    }
}
