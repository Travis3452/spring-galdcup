package com.example.galdcup.vote;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.vote.dto.CreateVoteSessionRequest;
import com.example.galdcup.vote.dto.VoteOptionDto;
import com.example.galdcup.vote.dto.VoteSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteSessionService {

    private final BoardValidator boardValidator;
    private final VoteSessionRepository voteSessionRepository;
    private final VoteSessionValidator voteSessionValidator;
    private final StringRedisTemplate redisTemplate;

    /** 투표 세션 생성 */
    @Transactional
    public VoteSessionDto createVoteSession(Long boardId, Long adminId, CreateVoteSessionRequest request) {
        Board board = boardValidator.validateAndGetActiveBoard(boardId);
        boardValidator.checkBoardManagerAuthority(board, adminId);
        voteSessionValidator.validateNoActiveVoteSession(board);

        VoteSession voteSession = voteSessionValidator.validateAndCreateVoteSession(board, request);

        board.setVoteSession(voteSession);

        VoteSession saved = voteSessionRepository.save(voteSession);
        return VoteSessionDto.from(saved);
    }

    /** 게시판의 현재 진행 중인 투표 세션 조회 */
    @Transactional(readOnly = true)
    public VoteSessionDto getVoteSession(Long boardId) {
        Board board = boardValidator.validateAndGetActiveBoard(boardId);

        VoteSession voteSession = voteSessionValidator.validateAndGetActiveVoteSession(board);

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
        VoteSession voteSession = voteSessionValidator.validateAndGetVoteSession(voteSessionId);

        voteSessionValidator.validateVoteSessionNotFinished(voteSession);

        voteSession.setFinished(true);
        VoteSession saved = voteSessionRepository.save(voteSession);

        return VoteSessionDto.from(saved);
    }
}