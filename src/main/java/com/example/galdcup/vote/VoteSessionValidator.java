package com.example.galdcup.vote;

import com.example.galdcup.board.Board;
import com.example.galdcup.vote.dto.CreateVoteSessionRequest;
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
     * 투표 생성 시 값 검증
     */
    public VoteSession validateAndCreateVoteSession(Board board, CreateVoteSessionRequest request) {
        OffsetDateTime startTime = request.startTime();
        OffsetDateTime endTime = request.endTime();

        List<String> options = request.options();
        List<String> optionImages = request.optionImages();

        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("투표 시작/종료 시간이 올바르지 않습니다.");
        }

        if (options == null || options.size() < 2 || options.size() > 50) {
            throw new IllegalArgumentException("투표 옵션은 최소 2개, 최대 50개까지 가능합니다.");
        }

        if (optionImages == null || optionImages.size() != options.size()) {
            throw new IllegalArgumentException("옵션 이미지 개수는 옵션 개수와 동일해야 합니다.");
        }

        VoteSession voteSession = VoteSession.builder()
                .board(board)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        List<VoteOption> voteOptions = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            VoteOption option = VoteOption.builder()
                    .voteSession(voteSession)
                    .label(options.get(i))
                    .imageUrl(optionImages.get(i))
                    .build();
            voteOptions.add(option);
        }

        voteSession.setOptions(voteOptions);

        return voteSession;
    }

    /**
     * 게시판에 진행 중인 VoteSession이 있는지 검증하고 반환
     */
    public VoteSession validateAndGetActiveVoteSession(Board board) {
        return voteSessionRepository.findByBoardAndIsFinishedFalse(board)
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

    public void validateVoteSessionNotFinished(VoteSession voteSession) {
        if (voteSession.isFinished()) {
            throw new IllegalStateException("이미 종료된 투표 세션입니다.");
        }
    }
}
