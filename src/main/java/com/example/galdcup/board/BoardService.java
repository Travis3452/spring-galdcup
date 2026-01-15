package com.example.galdcup.board;

import com.example.galdcup.board.dto.BoardDto;
import com.example.galdcup.board.dto.BoardPolicyDto;
import com.example.galdcup.board.dto.UpdateBoardPolicyRequest;
import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardPolicyRepository boardPolicyRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String BOARD_LATEST_KEY = "boards:latest";

    /**
     * 게시판 페이지 조회 (1페이지는 캐시 적용, TTL 5분)
     */
    @Transactional(readOnly = true)
    public Page<BoardDto> findAll(Pageable pageable) {
        if (pageable.getPageNumber() == 0) {
            String json = redisTemplate.opsForValue().get(BOARD_LATEST_KEY);
            if (json != null) {
                try {
                    return objectMapper.readValue(json, new TypeReference<PageImpl<BoardDto>>() {});
                } catch (Exception e) {
                    redisTemplate.delete(BOARD_LATEST_KEY);
                }
            }

            Page<Board> boards = boardRepository.findAll(pageable);
            Page<BoardDto> dtoPage = boards.map(BoardDto::from);

            try {
                String serialized = objectMapper.writeValueAsString(dtoPage);
                redisTemplate.opsForValue().set(BOARD_LATEST_KEY, serialized, 5, TimeUnit.MINUTES);
            } catch (Exception e) {
                // 직렬화 실패 시 캐싱 생략
            }

            return dtoPage;
        }

        return boardRepository.findAll(pageable).map(BoardDto::from);
    }

    /**
     * 특정 게시판 조회
     */
    @Transactional(readOnly = true)
    public Optional<BoardDto> findById(Long id) {
        return boardRepository.findById(id).map(BoardDto::from);
    }

    /**
     * 게시판 정책 조회
     */
    @Transactional(readOnly = true)
    public Optional<BoardPolicyDto> findPolicyByBoardId(Long boardId) {
        return boardRepository.findById(boardId)
                .map(Board::getBoardPolicy)
                .map(BoardPolicyDto::from);
    }

    /**
     * 게시판 정책 수정
     */
    @Transactional
    public BoardPolicyDto updatePolicy(Long boardId, UpdateBoardPolicyRequest request, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        BoardPolicy boardPolicy = board.getBoardPolicy();

        if (!boardPolicy.getBoardManager().getId().equals(userId)) {
            throw new AccessDeniedException("이 게시판의 관리자가 아닙니다.");
        }

        boardPolicy.setLikeThreshold(request.likeThreshold());

        return BoardPolicyDto.from(boardPolicy);
    }


    /**
     * 게시판 서브 매니저 추가
     */
    @Transactional
    public BoardPolicyDto addSubManager(Long boardId, String subManagerNickname, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        BoardPolicy boardPolicy = board.getBoardPolicy();

        if (!boardPolicy.getBoardManager().getId().equals(userId)) {
            throw new AccessDeniedException("이 게시판의 관리자가 아닙니다.");
        }

        User subManager = userRepository.findByNickname(subManagerNickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. nickname=" + subManagerNickname));

        // 중복 방지
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
    public BoardPolicyDto removeSubManager(Long boardId, String subManagerNickname, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        BoardPolicy boardPolicy = board.getBoardPolicy();

        if (!boardPolicy.getBoardManager().getId().equals(userId)) {
            throw new AccessDeniedException("이 게시판의 관리자가 아닙니다.");
        }

        User subManager = userRepository.findByNickname(subManagerNickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. nickname=" + subManagerNickname));

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
        User boardManager = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

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

        boardPolicyRepository.save(policy);
        board.setBoardPolicy(policy);

        Board saved = boardRepository.save(board);

        clearBoardCache();

        return BoardDto.from(saved);
    }

    /**
     * 게시판 상태 변경
     */
    @Transactional
    public BoardDto updateStatus(Long boardId, Board.Status newStatus, Long currentUserId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        if (!board.getBoardPolicy().getBoardManager().getId().equals(currentUserId)) {
            throw new AccessDeniedException("이 게시판의 관리자가 아닙니다.");
        }

        board.setStatus(newStatus);
        Board updated = boardRepository.save(board);

        clearBoardCache();

        return BoardDto.from(updated);
    }

    /**
     * 게시판 삭제
     */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        User boardManager = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        if (!board.getBoardPolicy().getBoardManager().getId().equals(boardManager.getId())) {
            throw new AccessDeniedException("이 게시판의 관리자가 아닙니다.");
        }

        boardRepository.delete(board);

        clearBoardCache();
    }

    /**
     * Redis 캐시 무효화
     */
    private void clearBoardCache() {
        redisTemplate.delete(BOARD_LATEST_KEY);
    }
}