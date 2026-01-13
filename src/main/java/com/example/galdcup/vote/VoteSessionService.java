package com.example.galdcup.vote;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.vote.dto.CreateVoteSessionRequest;
import com.example.galdcup.vote.dto.VoteOptionDto;
import com.example.galdcup.vote.dto.VoteSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteSessionService {

    private final BoardRepository boardRepository;
    private final VoteSessionRepository voteSessionRepository;
    private final StringRedisTemplate redisTemplate;

    /** 투표 세션 생성 */
    @Transactional
    public VoteSessionDto createVoteSession(Long boardId, Long adminId, CreateVoteSessionRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        if (!board.getBoardPolicy().getBoardManager().getId().equals(adminId)) {
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
        board.setVoteSession(voteSession);

        VoteSession saved = voteSessionRepository.save(voteSession);
        return VoteSessionDto.from(saved);
    }

    /** 투표 세션 조회 */
    @Transactional(readOnly = true)
    public VoteSessionDto getVoteSession(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        VoteSession voteSession = board.getVoteSession();
        if (voteSession == null) {
            throw new IllegalStateException("해당 게시판에는 투표 세션이 존재하지 않습니다.");
        }

        List<VoteOptionDto> voteOptionDtos = voteSession.getOptions().stream()
                .map(opt -> {
                    String key = "vote:" + voteSession.getId() + ":" + opt.getId();
                    String redisValue = redisTemplate.opsForValue().get(key);

                    Long count = (redisValue != null)
                            ? Long.valueOf(redisValue)
                            : opt.getCount();

                    return new VoteOptionDto(opt.getLabel(), opt.getImageUrl(), count);
                })
                .toList();

        return new VoteSessionDto(
                voteSession.getId(),
                board.getId(),
                voteSession.getStartTime(),
                voteSession.getEndTime(),
                voteOptionDtos
        );
    }

    /** 투표 세션 종료 처리 */
    @Transactional
    public VoteSessionDto finishVoteSession(Long voteSessionId) {
        VoteSession voteSession = voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new IllegalArgumentException("투표 세션을 찾을 수 없습니다."));

        if (voteSession.isFinished()) {
            throw new IllegalStateException("이미 종료된 투표 세션입니다.");
        }

        voteSession.setFinished(true);
        VoteSession saved = voteSessionRepository.save(voteSession);

        return VoteSessionDto.from(saved);
    }
}