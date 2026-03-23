package com.example.galdcup.voteSession.validator;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.vote.domain.VoteOption;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.domain.VoteSessionRepository;
import com.example.galdcup.voteSession.request.CreateVoteSessionRequest;
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
