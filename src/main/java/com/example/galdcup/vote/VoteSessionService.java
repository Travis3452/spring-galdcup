package com.example.galdcup.vote;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.vote.dto.CreateVoteSessionRequest;
import com.example.galdcup.vote.embedded.VoteOption;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteSessionService {

    private final BoardRepository boardRepository;
    private final VoteSessionRepository voteSessionRepository;

    /**
     * VoteSession 생성
     */
    public VoteSession createVoteSession(Long boardId, Long adminId, CreateVoteSessionRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        if (!board.getAdmin().getId().equals(adminId)) {
            throw new SecurityException("해당 게시판의 관리자만 투표 세션을 생성할 수 있습니다.");
        }

        if (board.getVoteSession() != null) {
            throw new IllegalStateException("이미 투표 세션이 존재합니다.");
        }

        OffsetDateTime startTime = request.startTime();
        OffsetDateTime endTime = request.endTime();

        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("투표 시작/종료 시간이 올바르지 않습니다.");
        }

        List<String> options = request.options();
        List<String> optionImages = request.optionImages();

        if (options == null || options.size() < 2 || options.size() > 50) {
            throw new IllegalArgumentException("투표 옵션은 최소 2개, 최대 50개까지 가능합니다.");
        }

        if (optionImages == null || optionImages.size() != options.size()) {
            throw new IllegalArgumentException("옵션 이미지 개수는 옵션 개수와 동일해야 합니다.");
        }

        List<VoteOption> voteOptions = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            voteOptions.add(new VoteOption(options.get(i), optionImages.get(i)));
        }

        VoteSession voteSession = VoteSession.builder()
                .board(board)
                .startTime(startTime)
                .endTime(endTime)
                .options(voteOptions)
                .build();

        board.setVoteSession(voteSession);

        return voteSessionRepository.save(voteSession);
    }

    /**
     * VoteSession 조회
     */
    public VoteSession getVoteSession(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        VoteSession voteSession = board.getVoteSession();
        if (voteSession == null) {
            throw new IllegalStateException("해당 게시판에는 투표 세션이 존재하지 않습니다.");
        }
        return voteSession;
    }
}
