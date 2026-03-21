package com.example.galdcup.voteSession.validator;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.vote.domain.VoteOption;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.domain.VoteSessionRepository;
import com.example.galdcup.voteSession.dto.CreateVoteSessionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VoteSessionValidator {
    private final VoteSessionRepository voteSessionRepository;

    /**
     * VoteSession 생성 시 값 검증 및 엔티티 생성 (객체 리스트 방식)
     */
    public VoteSession validateAndCreateVoteSession(Board board, CreateVoteSessionRequest request) {
        OffsetDateTime startTime = request.startTime();
        OffsetDateTime endTime = request.endTime();

        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("투표 시작/종료 시간이 올바르지 않습니다.");
        }

        if (request.options() == null || request.options().size() < 2 || request.options().size() > 50) {
            throw new IllegalArgumentException("투표 옵션은 최소 2개, 최대 50개까지 가능합니다.");
        }

        VoteSession voteSession = VoteSession.builder()
                .board(board)
                .startTime(startTime)
                .endTime(endTime)
                .options(new ArrayList<>())
                .build();

        List<VoteOption> voteOptions = request.options().stream()
                .map(opt -> VoteOption.builder()
                        .voteSession(voteSession)
                        .label(opt.label())
                        .imageUrl(opt.imageUrl())
                        .count(0L)
                        .build())
                .toList();

        voteSession.getOptions().addAll(voteOptions);

        return voteSession;
    }

    /**
     * 게시판에 진행 중인 VoteSession이 있는지 검증하고 반환
     */
    public VoteSession validateAndGetActiveVoteSession(Long boardId) {
        return voteSessionRepository.findByBoardIdAndIsFinishedFalse(boardId)
                .orElseThrow(() -> new IllegalStateException("현재 진행 중인 투표 세션이 존재하지 않습니다."));
    }

    /**
     * 게시판에 진행 중인 VoteSession이 있는지 검증
     */
    public void validateNoActiveVoteSession(Board board) {
        if (voteSessionRepository.existsByBoardAndIsFinishedFalse(board)) {
            throw new IllegalStateException("지금 진행 중인 투표 세션이 존재합니다.");
        }
    }

    /**
     * 투표 세션이 존재하는지 검증
     */
    public VoteSession validateAndGetVoteSession(Long voteSessionId) {
        return voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new IllegalArgumentException("투표 세션을 찾을 수 없습니다."));
    }

    /**
     * 올바른 투표인지 검증
     */
    public void validateVote(VoteSession voteSession, int selectedOptionIndex, OffsetDateTime now) {

        if (now.isBefore(voteSession.getStartTime()) || now.isAfter(voteSession.getEndTime())) {
            throw new IllegalStateException("현재는 투표 가능 시간이 아닙니다.");
        }

        if (selectedOptionIndex < 0 || selectedOptionIndex >= voteSession.getOptions().size()) {
            throw new IllegalArgumentException("잘못된 투표 옵션입니다.");
        }
    }
}
