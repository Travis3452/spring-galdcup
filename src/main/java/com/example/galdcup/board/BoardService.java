package com.example.galdcup.board;

import com.example.galdcup.board.domain.Board;
import com.example.galdcup.board.domain.BoardManagerRequest;
import com.example.galdcup.board.domain.BoardManagerRequestRepository;
import com.example.galdcup.board.domain.BoardPolicy;
import com.example.galdcup.board.domain.BoardRepository;
import com.example.galdcup.board.event.BoardChangedEvent;
import com.example.galdcup.board.redis.BoardRedisManager;
import com.example.galdcup.board.request.BoardRequest;
import com.example.galdcup.board.response.BoardDetailResponse;
import com.example.galdcup.board.response.BoardDto;
import com.example.galdcup.board.response.BoardManagerRequestDto;
import com.example.galdcup.board.response.BoardPolicyDto;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.postCategory.PostCategoryService;
import com.example.galdcup.postCategory.dto.PostCategoryDto;
import com.example.galdcup.user.User;
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

    /** 전체 게시판 목록 조회 (첫 페이지 캐시 적용) */
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

    /** 랭킹 시스템에 의한 인기 게시판 목록 조회 */
    @Transactional(readOnly = true)
    public List<BoardDto> getPopularBoards() {
        return boardRedisManager.getBoardList("ranking")
                .orElseGet(() -> boardRepository.findByStatus(Board.Status.OPEN).stream()
                        .limit(10).map(BoardDto::from).toList());
    }

    /** 단일 게시판 조회 및 조회수 증가 처리 */
    @Transactional
    public BoardDto findById(Long id) {
        Board board = boardValidator.findByIdOrThrow(id);
        if (board.getStatus() == Board.Status.OPEN) {
            boardRedisManager.incrementViewCount(board.getId());
        }
        return BoardDto.from(board);
    }

    /** 게시판 상세 메타데이터(정책, 카테고리 포함) 통합 조회 */
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

    /** 키워드 기반 게시판 검색 */
    @Transactional(readOnly = true)
    public Page<BoardDto> getBoardsByKeyword(Pageable pageable, String keyword) {
        return boardRepository.searchBoards(keyword, pageable).map(BoardDto::from);
    }

    // ==========================================
    // 2. 게시판 생성 및 상태 관리
    // ==========================================

    /** 신규 게시판 생성 및 초기 설정 */
    @Transactional
    public BoardDto create(BoardRequest.Create request, Long currentUserId) {
        User boardManager = userValidator.findByIdOrThrow(currentUserId);
        Board board = Board.builder()
                .topic(request.topic())
                .description(request.description())
                .status(Board.Status.OPEN).build();

        board.setBoardPolicy(BoardPolicy.builder().board(board).boardManager(boardManager).likeThreshold(20).build());
        board.setDefaultCategories();

        boardRepository.save(board);
        clearBoardCache(null);
        return BoardDto.from(board);
    }

    /** 게시판 운영 상태 변경 */
    @Transactional
    public BoardDto updateStatus(Long boardId, BoardRequest.UpdateStatus request, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.setStatus(request.status());
        clearBoardCache(boardId);
        return BoardDto.from(board);
    }

    /** 게시판 폐쇄 처리 */
    @Transactional
    public void delete(Long boardId, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.setStatus(Board.Status.CLOSED);
        clearBoardCache(boardId);
    }

    // ==========================================
    // 3. 권한 위임 및 정책
    // ==========================================

    /** 현재 메인 관리자가 특정 유저에게 자신의 관리자 권한을 완전히 위임합니다. */
    @Transactional
    public BoardPolicyDto delegateManager(Long boardId, String targetNickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        User newManager = userValidator.findByNicknameOrThrow(targetNickname);

        if (newManager.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("자기 자신에게는 권한을 위임할 수 없습니다.");
        }

        board.getBoardPolicy().getSubManagers().remove(newManager);
        board.getBoardPolicy().setBoardManager(newManager);

        clearBoardCache(boardId);

        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** 관리자 위임 신청 처리 (중복 검증 및 공석 시 자동 승인 포함) */
    @Transactional
    public BoardManagerRequestDto applyForManager(Long boardId, Long userId) {
        Board board = boardValidator.findBoardWithPolicyById(boardId);
        User applicant = userValidator.findByIdOrThrow(userId);

        boardValidator.validateNoPendingRequest(userId, boardId);

        BoardManagerRequest.Status status = BoardManagerRequest.Status.PENDING;

        if (board.getBoardPolicy().getBoardManager() == null) {
            boolean isSubManager = boardValidator.isSubManager(board, applicant);
            boolean noSubManagers = !boardValidator.hasAnySubManager(board);

            if (isSubManager || noSubManagers) {
                status = BoardManagerRequest.Status.APPROVED;
                board.getBoardPolicy().setBoardManager(applicant);
                if (isSubManager) {
                    board.getBoardPolicy().getSubManagers().remove(applicant);
                }
                clearBoardCache(boardId);
            }
        }

        BoardManagerRequest request = BoardManagerRequest.builder()
                .applicant(applicant).board(board).status(status).build();

        return BoardManagerRequestDto.from(boardManagerRequestRepository.save(request));
    }

    /** 게시판 운영 정책 수정 */
    @Transactional
    public BoardPolicyDto updatePolicy(Long boardId, BoardRequest.UpdatePolicy request, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.getBoardPolicy().setLikeThreshold(request.likeThreshold());
        clearBoardCache(boardId);
        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** 서브 매니저 추가 */
    @Transactional
    public BoardPolicyDto addSubManager(Long boardId, String nickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        User subManager = userValidator.findByNicknameOrThrow(nickname);

        if (board.getBoardPolicy().getSubManagers().contains(subManager)) {
            throw new IllegalArgumentException("이미 등록된 서브 매니저입니다.");
        }

        board.getBoardPolicy().getSubManagers().add(subManager);
        clearBoardCache(boardId);
        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** 서브 매니저 해임 */
    @Transactional
    public BoardPolicyDto removeSubManager(Long boardId, String nickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        User subManager = userValidator.findByNicknameOrThrow(nickname);

        board.getBoardPolicy().getSubManagers().remove(subManager);
        clearBoardCache(boardId);
        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    // ==========================================
    // 4. 내부 유틸리티
    // ==========================================

    /** 변경 사항 발생 시 캐시 무효화 및 이벤트 전파 */
    private void clearBoardCache(Long boardId) {
        boardRedisManager.deleteBoardListCache();
        if (boardId != null) {
            boardRedisManager.deleteBoardDetailCache(boardId);
            eventPublisher.publishEvent(new BoardChangedEvent(boardId));
        }
    }
}