package com.example.galdcup.board;

import com.example.galdcup.board.dto.BoardDto;
import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BOARD_PAGE_KEY_PREFIX = "boards:page:";

    /**
     * 전체 게시판 조회 (캐시 적용, TTL 1분)
     */
    @Transactional(readOnly = true)
    public Page<BoardDto> findAll(Pageable pageable) {
        String key = BOARD_PAGE_KEY_PREFIX + pageable.getPageNumber() + ":"
                + pageable.getPageSize() + ":"
                + pageable.getSort();

        Page<BoardDto> cachedPage = (Page<BoardDto>) redisTemplate.opsForValue().get(key);
        if (cachedPage != null) {
            return cachedPage;
        }

        Page<Board> boards = boardRepository.findAll(pageable);
        Page<BoardDto> dtoPage = boards.map(BoardDto::from);

        redisTemplate.opsForValue().set(key, dtoPage, 1, TimeUnit.MINUTES);
        return dtoPage;
    }

    /**
     * 특정 게시판 조회
     */
    @Transactional(readOnly = true)
    public Optional<BoardDto> findById(Long id) {
        return boardRepository.findById(id).map(BoardDto::from);
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
                .boardManager(boardManager)
                .status(Board.Status.OPEN)
                .build();

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

        if (!board.getBoardManager().getId().equals(currentUserId)) {
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

        if (!board.getBoardManager().getId().equals(boardManager.getId())) {
            throw new AccessDeniedException("이 게시판의 관리자가 아닙니다.");
        }

        boardRepository.delete(board);

        clearBoardCache();
    }

    /**
     * Redis 캐시 무효화
     */
    private void clearBoardCache() {
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(ScanOptions.scanOptions().match(BOARD_PAGE_KEY_PREFIX + "*").count(100).build())) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                redisTemplate.delete(key);
            }
        }
    }
}