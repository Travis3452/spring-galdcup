package com.example.galdcup.board;

import com.example.galdcup.board.domain.*;
import com.example.galdcup.board.event.BoardChangedEvent;
import com.example.galdcup.board.redis.BoardRedisManager;
import com.example.galdcup.board.request.BoardRequest;
import com.example.galdcup.board.response.BoardDetailResponse;
import com.example.galdcup.board.response.BoardDto;
import com.example.galdcup.board.response.BoardManagerRequestDto;
import com.example.galdcup.board.response.BoardPolicyDto;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.postCategory.PostCategoryService;
import com.example.galdcup.postCategory.response.PostCategoryDto;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardManagerRequestRepository boardManagerRequestRepository;
    private final BoardValidator boardValidator;
    private final UserValidator userValidator;
    private final PostCategoryService postCategoryService;
    private final BoardRedisManager boardRedisManager;
    private final ApplicationEventPublisher eventPublisher;

    // ==========================================
    // 1. 게시판 조회
    // ==========================================

    /** 전체 게시판 목록을 페이징하여 조회.(1페이지 캐싱 적용) */
    @Transactional(readOnly = true)
    public Page<BoardDto> findAll(Pageable pageable) {
        if (pageable.getPageNumber() == 0) {
            return boardRedisManager.getBoardList("latest")
                    .map(list -> new PageImpl<>(list, pageable, list.size()))
                    .orElseGet(() -> {
                        Page<Board> boards = boardRepository.findByStatus(Board.Status.OPEN, pageable);
                        List<BoardDto> dtoList = boards.getContent().stream().map(BoardDto::from).toList();
                        boardRedisManager.saveBoardList("latest", dtoList);
                        return new PageImpl<>(dtoList, pageable, boards.getTotalElements());
                    });
        }
        return boardRepository.findByStatus(Board.Status.OPEN, pageable).map(BoardDto::from);
    }

    /** Redis 조회수 기반의 인기 게시판 목록을 조회. */
    @Transactional(readOnly = true)
    public List<BoardDto> getPopularBoards() {
        return boardRedisManager.getBoardList("ranking")
                .orElseGet(() -> boardRepository.findByStatus(Board.Status.OPEN).stream()
                        .limit(10).map(BoardDto::from).toList());
    }

    /** 특정 게시판을 조회하며, OPEN 상태인 게시판인 경우 조회수를 증가. */
    @Transactional
    public BoardDto findById(Long id) {
        Board board = boardValidator.findByIdOrThrow(id);
        if (board.getStatus() == Board.Status.OPEN) {
            boardRedisManager.incrementViewCount(board.getId());
        }
        return BoardDto.from(board);
    }

    /** 게시판 정보, 관리 정책, 카테고리 목록을 통합하여 상세 응답을 생성.(캐싱 적용) */
    @Transactional(readOnly = true)
    public BoardDetailResponse getBoardDetail(Long boardId) {
        return boardRedisManager.getBoardDetail(boardId)
                .orElseGet(() -> {
                    Board board = boardValidator.findBoardWithPolicyById(boardId);
                    List<PostCategoryDto> categories = postCategoryService.findByBoardId(boardId);
                    BoardDetailResponse response = BoardDetailResponse.builder()
                            .board(BoardDto.from(board))
                            .policy(BoardPolicyDto.from(board.getBoardPolicy()))
                            .categories(categories)
                            .build();
                    boardRedisManager.saveBoardDetail(boardId, response);
                    return response;
                });
    }

    /** 검색어를 통해 게시판을 조회. */
    @Transactional(readOnly = true)
    public Page<BoardDto> getBoardsByKeyword(Pageable pageable, String keyword) {
        return boardRepository.searchBoards(keyword, pageable).map(BoardDto::from);
    }

    // ==========================================
    // 2. 게시판 생성 및 상태 관리
    // ==========================================

    /** 새로운 게시판을 생성하고 기본 정책과 카테고리를 설정. */
    @Transactional
    public BoardDto create(BoardRequest.Create request, Long currentUserId) {
        User boardManager = userValidator.findByIdOrThrow(currentUserId);

        Board board = Board.create(request.topic(), request.description());
        board.assignPolicy(BoardPolicy.create(board, boardManager));
        board.setDefaultCategories();

        boardRepository.save(board);
        clearBoardCache(null);

        return BoardDto.from(board);
    }

    /** 게시판의 운영 상태(OPEN/CLOSED)를 변경. */
    @Transactional
    public BoardDto updateStatus(Long boardId, BoardRequest.UpdateStatus request, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.changeStatus(request.status());
        clearBoardCache(boardId);
        return BoardDto.from(board);
    }

    /** 게시판의 운영 상태를 CLOSE로 변경하고 관련 캐시를 삭제. */
    @Transactional
    public void delete(Long boardId, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.closeBoard();
        clearBoardCache(boardId);
    }

    // ==========================================
    // 3. 게시판 정책 및 권한 관리
    // ==========================================

    /** 게시판 관리자 권한을 다른 유저에게 위임. */
    @Transactional
    public BoardPolicyDto delegateManager(Long boardId, String targetNickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        User newManager = userValidator.findByNicknameOrThrow(targetNickname);

        board.getBoardPolicy().delegateMainManager(newManager);
        clearBoardCache(boardId);

        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** * 게시판 관리자 권한을 신청.
     * @implNote 소유자가 없는 게시판에 서브 매니저가 신청할 경우 즉시 승인 처리
     */
    @Transactional
    public BoardManagerRequestDto applyForManager(Long boardId, Long userId) {
        Board board = boardValidator.findBoardWithPolicyById(boardId);
        User applicant = userValidator.findByIdOrThrow(userId);

        boardValidator.validateNoPendingRequest(userId, boardId);

        BoardManagerRequest.Status status = BoardManagerRequest.Status.PENDING;

        // 관리자 자동 승인 로직: 소유자가 공석인 경우의 처리
        if (board.getBoardPolicy().getBoardManager() == null) {
            boolean isSubManager = board.getBoardPolicy().isSubManager(applicant);
            boolean noSubManagers = !boardValidator.hasAnySubManager(board);

            if (isSubManager || noSubManagers) {
                status = BoardManagerRequest.Status.APPROVED;
                board.getBoardPolicy().delegateMainManager(applicant);
                clearBoardCache(boardId);
            }
        }

        BoardManagerRequest request = BoardManagerRequest.create(board, applicant, status);
        return BoardManagerRequestDto.from(boardManagerRequestRepository.save(request));
    }

    /** 게시판 정책을 수정. */
    @Transactional
    public BoardPolicyDto updatePolicy(Long boardId, BoardRequest.UpdatePolicy request, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.getBoardPolicy().updateLikeThreshold(request.likeThreshold());
        clearBoardCache(boardId);
        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** 게시판의 서브 매니저 권한 임명. */
    @Transactional
    public BoardPolicyDto addSubManager(Long boardId, String nickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        User subManager = userValidator.findByNicknameOrThrow(nickname);

        board.getBoardPolicy().addSubManager(subManager);
        clearBoardCache(boardId);

        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** 게시판의 서브 매니저 권한 제거. */
    @Transactional
    public BoardPolicyDto removeSubManager(Long boardId, String nickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        User subManager = userValidator.findByNicknameOrThrow(nickname);

        board.getBoardPolicy().removeSubManager(subManager);
        clearBoardCache(boardId);
        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    // ==========================================
    // 헬퍼 메서드
    // ==========================================

    /** 게시판 정보 변경 시 관련 Redis 캐시를 삭제하고 이벤트를 발행. */
    private void clearBoardCache(Long boardId) {
        boardRedisManager.deleteBoardListCache();
        if (boardId != null) {
            boardRedisManager.deleteBoardDetailCache(boardId);
            eventPublisher.publishEvent(new BoardChangedEvent(boardId));
        }
    }
}