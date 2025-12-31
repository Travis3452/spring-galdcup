package com.example.galdcup.service;

import com.example.galdcup.dto.board.BoardDto;
import com.example.galdcup.entity.Board;
import com.example.galdcup.entity.User;
import com.example.galdcup.repository.BoardRepository;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BOARD_PAGE_KEY_PREFIX = "boards:page:";

    // 전체 게시판 조회
    @Transactional(readOnly = true)
    public Page<BoardDto> findAll(Pageable pageable) {
        String key = BOARD_PAGE_KEY_PREFIX + pageable.getPageNumber() + ":"
                + pageable.getPageSize() + ":"
                + pageable.getSort();

        // Redis 캐시 조회
        Page<BoardDto> cachedPage = (Page<BoardDto>) redisTemplate.opsForValue().get(key);
        if (cachedPage != null) {
            return cachedPage;
        }

        // DB 조회 후 캐싱
        Page<Board> boards = boardRepository.findAll(pageable);
        Page<BoardDto> dtoPage = boards.map(BoardDto::from);

        redisTemplate.opsForValue().set(key, dtoPage, 10, TimeUnit.MINUTES); // TTL 10분
        return dtoPage;
    }

    // 특정 게시판 조회
    @Transactional(readOnly = true)
    public Optional<BoardDto> findById(Long id) {
        return boardRepository.findById(id).map(BoardDto::from);
    }

    // 게시판 생성
    @Transactional
    public BoardDto create(String topic, String description, String adminOauthId) {
        User admin = userRepository.findByOauthId(adminOauthId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        Board board = Board.builder()
                .topic(topic)
                .description(description)
                .admin(admin)
                .status(Board.Status.OPEN)
                .build();

        Board saved = boardRepository.save(board);

        redisTemplate.delete(BOARD_PAGE_KEY_PREFIX + "*");

        return BoardDto.from(saved);
    }

    // 게시판 상태 변경
    @Transactional
    public BoardDto updateStatus(Long boardId, Board.Status newStatus, String currentUserOauthId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        if (!board.getAdmin().getOauthId().equals(currentUserOauthId)) {
            throw new AccessDeniedException("이 게시판의 관리자가 아닙니다.");
        }

        board.setStatus(newStatus);
        Board updated = boardRepository.save(board);

        // 캐시 무효화
        redisTemplate.delete(BOARD_PAGE_KEY_PREFIX + "*");

        return BoardDto.from(updated);
    }

    // 게시판 삭제
    @Transactional
    public void delete(Long id, String oauthId) {
        User admin = userRepository.findByOauthId(oauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        boardRepository.delete(board);

        // 캐시 무효화
        redisTemplate.delete(BOARD_PAGE_KEY_PREFIX + "*");
    }
}