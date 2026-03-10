package com.example.galdcup.board;

import com.example.galdcup.board.dto.BoardDto;
import com.example.galdcup.board.dto.BoardListResponse;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.boardPolicy.BoardPolicy;
import com.example.galdcup.boardPolicy.BoardPolicyRepository;
import com.example.galdcup.boardPolicy.dto.BoardPolicyDto;
import com.example.galdcup.boardPolicy.dto.UpdateBoardPolicyRequest;
import com.example.galdcup.user.User;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardPolicyRepository boardPolicyRepository;

    private final BoardValidator boardValidator;
    private final UserValidator userValidator;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BOARD_LATEST_KEY = "boards:latest";
    private static final String BOARD_VIEWS_KEY = "boards:views";

    /**
     * 게시판 페이지 조회 (TTL 5분)
     */
    @Transactional(readOnly = true)
    public Page<BoardDto> findAll(Pageable pageable) {
        if (pageable.getPageNumber() == 0) {
            BoardListResponse cachedResponse = (BoardListResponse) redisTemplate.opsForValue().get(BOARD_LATEST_KEY);

            if (cachedResponse != null) {
                List<BoardDto> cachedList = cachedResponse.getBoardDtos();
                return new PageImpl<>(cachedList, pageable, cachedList.size());
            }

            Page<Board> boards = boardRepository.findByStatus(Board.Status.OPEN, pageable);
            List<BoardDto> dtoList = boards.getContent().stream()
                    .map(BoardDto::from)
                    .toList();

            redisTemplate.opsForValue().set(BOARD_LATEST_KEY, new BoardListResponse(dtoList), Duration.ofMinutes(5));

            return new PageImpl<>(dtoList, pageable, boards.getTotalElements());
        }

        return boardRepository.findByStatus(Board.Status.OPEN, pageable)
                .map(BoardDto::from);
    }

    /**
     * 인기 게시판 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BoardDto> getPopularBoards() {
        BoardListResponse cachedResponse = (BoardListResponse) redisTemplate.opsForValue().get(BOARD_LATEST_KEY);

        if (cachedResponse != null) {
            return cachedResponse.getBoardDtos();
        }

        return boardRepository.findByStatus(Board.Status.OPEN)
                .stream()
                .map(BoardDto::from)
                .toList();
    }

    /**
     * 특정 게시판 조회
     */
    @Transactional(readOnly = true)
    public Optional<BoardDto> findById(Long id) {
        Optional<Board> boardOpt = boardRepository.findById(id);

        boardOpt.ifPresent(board -> {
            try {
                if (board.getStatus() == Board.Status.OPEN) {
                    redisTemplate.opsForZSet().incrementScore(BOARD_VIEWS_KEY, board.getId().toString(), 1);
                }
            } catch (Exception e) {
                log.error("Redis error during view count increment: {}", e.getMessage());
            }
        });

        return boardOpt.map(BoardDto::from);
    }

    /**
     * 게시판 검색(topic)
     */
    @Transactional(readOnly = true)
    public Page<BoardDto> getBoardsByKeyword(Pageable pageable, String keyword) {
        Page<Board> boardPage = boardRepository.searchBoards(keyword, pageable);

        return boardPage.map(BoardDto::from);
    }

    /**
     * 게시판 정책 조회
     */
    @Transactional(readOnly = true)
    public Optional<BoardPolicyDto> findPolicyByBoardId(Long boardId) {
        return boardPolicyRepository.findByBoardId(boardId)
                .map(BoardPolicyDto::from);
    }

    /**
     * 게시판 정책 수정
     */
    @Transactional
    public BoardPolicyDto updatePolicy(Long boardId, UpdateBoardPolicyRequest request, Long currentUserId) {
        Board board =  boardValidator.getBoardIfBoardManager(boardId, currentUserId);

        BoardPolicy boardPolicy = board.getBoardPolicy();
        boardPolicy.setLikeThreshold(request.likeThreshold());

        return BoardPolicyDto.from(boardPolicy);
    }


    /**
     * 게시판 서브 매니저 추가
     */
    @Transactional
    public BoardPolicyDto addSubManager(Long boardId, String subManagerNickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);

        BoardPolicy boardPolicy = board.getBoardPolicy();

        User subManager = userValidator.findByNicknameOrThrow(subManagerNickname);

        if (boardPolicy.getSubManagers().stream().anyMatch(u -> u.getId().equals(subManager.getId()))) {
            throw new IllegalArgumentException("이미 서브 매니저로 등록된 사용자입니다.");
        }

        boardPolicy.getSubManagers().add(subManager);
        return BoardPolicyDto.from(boardPolicy);
    }

    /**
     * 게시판 서브 매니저 삭제
     */
    @Transactional
    public BoardPolicyDto removeSubManager(Long boardId, String subManagerNickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);

        BoardPolicy boardPolicy = board.getBoardPolicy();

        User subManager = userValidator.findByNicknameOrThrow(subManagerNickname);

        boolean removed = boardPolicy.getSubManagers()
                .removeIf(user -> user.getId().equals(subManager.getId()));

        if (!removed) {
            throw new IllegalArgumentException("해당 사용자는 서브 매니저가 아닙니다.");
        }

        return BoardPolicyDto.from(boardPolicy);
    }


    /**
     * 게시판 생성
     */
    @Transactional
    public BoardDto create(String topic, String description, Long currentUserId) {
        User boardManager = userValidator.findByIdOrThrow(currentUserId);

        Board board = Board.builder()
                .topic(topic)
                .description(description)
                .status(Board.Status.OPEN)
                .build();

        BoardPolicy policy = BoardPolicy.builder()
                .board(board)
                .boardManager(boardManager)
                .likeThreshold(20)
                .build();

        board.setBoardPolicy(policy);
        board.setDefaultCategories();

        boardRepository.save(board);
        clearBoardCache();

        return BoardDto.from(board);
    }

    /**
     * 게시판 상태 변경
     */
    @Transactional
    public BoardDto updateStatus(Long boardId, Board.Status newStatus, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);

        board.setStatus(newStatus);
        Board updated = boardRepository.save(board);

        clearBoardCache();

        return BoardDto.from(updated);
    }

    /**
     * 게시판 삭제
     */
    @Transactional
    public void delete(Long boardId, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);

        board.setStatus(Board.Status.CLOSED);

        clearBoardCache();
    }

    /**
     * Redis 캐시 무효화
     */
    private void clearBoardCache() {
        redisTemplate.delete(BOARD_LATEST_KEY);
    }
}