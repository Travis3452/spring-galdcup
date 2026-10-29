package com.example.galdcup.board.voteSession.validator;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.voteSession.domain.VoteSession;
import com.example.galdcup.board.voteSession.domain.VoteSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class VoteSessionValidator {
    private final VoteSessionRepository voteSessionRepository;

    /** VoteSession의 존재를 검증하고 반환 */
    public VoteSession validateAndGetVoteSessionWithOptionsAndBoard(Long voteSessionId) {
        return voteSessionRepository.findWithOptionsAndBoardById(voteSessionId)
                .orElseThrow(() -> new IllegalArgumentException("투표 세션을 찾을 수 없습니다."));
    }

    /** 게시판에 진행 중인 VoteSession이 있는지 검증하고 반환 */
    public VoteSession validateAndGetActiveVoteSession(Long boardId) {
        return voteSessionRepository.findByBoardIdAndIsFinishedFalse(boardId)
                .orElseThrow(() -> new IllegalStateException("현재 진행 중인 투표 세션이 존재하지 않습니다."));
    }

    /** 요청한 세션의 소속, 게시판 상태, 투표 시간 및 선택지를 함께 검증 */
    public VoteSession validateAndGetVotingSession(Long boardId, Long voteSessionId,
                                                   int optionIndex, OffsetDateTime now) {
        VoteSession session = validateAndGetVoteSessionWithOptionsAndBoard(voteSessionId);
        if (!session.getBoard().getId().equals(boardId)) {
            throw new IllegalArgumentException("해당 게시판에 속하지 않는 투표 세션입니다.");
        }
        if (session.getBoard().getStatus() != Board.Status.OPEN) {
            throw new IllegalStateException("현재 폐쇄된 게시판입니다.");
        }
        if (!session.isActiveAt(now)) {
            throw new IllegalStateException("현재 투표할 수 없는 세션입니다.");
        }
        if (optionIndex < 0 || optionIndex >= session.getOptions().size()) {
            throw new IllegalArgumentException("유효하지 않은 투표 선택지입니다.");
        }
        return session;
    }

    /**
     * 게시판에 진행 중인 VoteSession 존재 여부 확인
     */
    public void validateNoActiveVoteSession(Board board) {
        if (voteSessionRepository.existsByBoardAndIsFinishedFalse(board)) {
            throw new IllegalStateException("지금 진행 중인 투표 세션이 존재합니다.");
        }
    }

    /** 투표 세션 및 옵션 목록을 한꺼번에 조회 */
    public VoteSession validateAndGetVoteSessionWithOptions(Long voteSessionId) {
        return voteSessionRepository.findVoteSessionWithOptionsById(voteSessionId)
                .orElseThrow(() -> new IllegalArgumentException("투표 세션을 찾을 수 없습니다."));
    }
}
