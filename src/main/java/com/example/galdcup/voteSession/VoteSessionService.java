package com.example.galdcup.voteSession;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.vote.VoteOption;
import com.example.galdcup.vote.dto.VoteOptionDto;
import com.example.galdcup.voteSession.dto.CreateVoteSessionRequest;
import com.example.galdcup.voteSession.dto.VoteSessionDto;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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

        voteSession.setBoard(board);
        board.getVoteSessions().add(voteSession);

        VoteSession saved = voteSessionRepository.save(voteSession);
        return VoteSessionDto.from(saved);
    }

    /** 게시판의 현재 진행 중인 투표 세션 조회 */
    @Transactional(readOnly = true)
    public VoteSessionDto getVoteSession(Long boardId) {
        Board board = boardValidator.validateAndGetActiveBoard(boardId);
        VoteSession voteSession = voteSessionValidator.validateAndGetActiveVoteSession(board);

        String hashKey = "voteSession:count:" + voteSession.getId();
        Map<Object, Object> votes = redisTemplate.opsForHash().entries(hashKey);

        List<VoteOptionDto> voteOptionDtos = IntStream.range(0, voteSession.getOptions().size())
                .mapToObj(i -> {
                    VoteOption opt = voteSession.getOptions().get(i);

                    Object redisValue = votes.get(String.valueOf(i));

                    Long count = (redisValue != null)
                            ? Long.parseLong(redisValue.toString())
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