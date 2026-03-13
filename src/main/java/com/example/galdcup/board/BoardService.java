package com.example.galdcup.board;

import com.example.galdcup.board.dto.BoardDetailResponse;
import com.example.galdcup.board.dto.BoardDto;
import com.example.galdcup.board.event.BoardChangedEvent;
import com.example.galdcup.board.validator.BoardValidator;
import com.example.galdcup.boardPolicy.BoardPolicy;
import com.example.galdcup.boardPolicy.dto.BoardPolicyDto;
import com.example.galdcup.boardPolicy.dto.UpdateBoardPolicyRequest;
import com.example.galdcup.postCategory.PostCategoryService;
import com.example.galdcup.postCategory.dto.PostCategoryDto;
import com.example.galdcup.user.User;
import com.example.galdcup.user.validator.UserValidator;
import com.example.galdcup.voteSession.VoteSessionService;
import com.example.galdcup.voteSession.dto.VoteSessionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardValidator boardValidator;
    private final BoardViewService boardViewService;

    private final UserValidator userValidator;
    private final PostCategoryService postCategoryService;
    private final VoteSessionService voteSessionService;

    private final BoardRedisManager boardRedisManager;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 게시판 페이지 조회 (첫 페이지 캐싱)
     */
    @Transactional(readOnly = true)
    public Page<BoardDto> findAll(Pageable pageable) {
        if (pageable.getPageNumber() == 0) {
            return boardRedisManager.getBoardList("latest")
                    .map(dtoList -> new PageImpl<>(dtoList, pageable, dtoList.size()))
                    .orElseGet(() -> {
                        Page<Board> boards = boardRepository.findByStatus(Board.Status.OPEN, pageable);
                        List<BoardDto> dtoList = boards.getContent().stream()
                                .map(BoardDto::from)
                                .toList();

                        boardRedisManager.saveBoardList("latest", dtoList);
                        return new PageImpl<>(dtoList, pageable, boards.getTotalElements());
                    });
        }

        return boardRepository.findByStatus(Board.Status.OPEN, pageable)
                .map(BoardDto::from);
    }

    /**
     * 인기 게시판 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BoardDto> getPopularBoards() {
        return boardRedisManager.getBoardList("latest")
                .orElseGet(() -> {
                    List<BoardDto> dtoList = boardRepository.findByStatus(Board.Status.OPEN)
                            .stream()
                            .map(BoardDto::from)
                            .toList();
                    boardRedisManager.saveBoardList("latest", dtoList);
                    return dtoList;
                });
    }

    /**
     * 게시판 상세 데이터 통합 조회 (캐싱 적용)
     */
    @Transactional(readOnly = true)
    public BoardDetailResponse getBoardDetail(Long boardId) {
        return boardRedisManager.getBoardDetail(boardId)
                .orElseGet(() -> {
                    Board board = boardValidator.findBoardWithPolicyById(boardId);
                    VoteSessionDto activeVoteSession = voteSessionService.getActiveVoteSession(boardId)
                            .orElse(null);
                    List<PostCategoryDto> categories = postCategoryService.findByBoardId(boardId);

                    BoardDetailResponse response = BoardDetailResponse.builder()
                            .board(BoardDto.from(board))
                            .policy(BoardPolicyDto.from(board.getBoardPolicy()))
                            .categories(categories)
                            .activeVoteSession(activeVoteSession)
                            .build();

                    boardRedisManager.saveBoardDetail(boardId, response);
                    return response;
                });
    }

    /**
     * 특정 게시판 조회 (조회수 증가 포함)
     */
    @Transactional(readOnly = true)
    public Optional<BoardDto> findById(Long id) {
        Optional<Board> boardOpt = boardRepository.findById(id);
        boardOpt.ifPresent(board -> {
            if (board.getStatus() == Board.Status.OPEN) {
                boardViewService.incrementViewCount(board.getId());
            }
        });
        return boardOpt.map(BoardDto::from);
    }

    /**
     * 게시판 검색
     */
    @Transactional(readOnly = true)
    public Page<BoardDto> getBoardsByKeyword(Pageable pageable, String keyword) {
        return boardRepository.searchBoards(keyword, pageable).map(BoardDto::from);
    }

    /**
     * 게시판 정책 수정
     */
    @Transactional
    public BoardPolicyDto updatePolicy(Long boardId, UpdateBoardPolicyRequest request, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.getBoardPolicy().setLikeThreshold(request.likeThreshold());

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));

        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /**
     * 서브 매니저 추가
     */
    @Transactional
    public BoardPolicyDto addSubManager(Long boardId, String subManagerNickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        User subManager = userValidator.findByNicknameOrThrow(subManagerNickname);

        if (board.getBoardPolicy().getSubManagers().stream().anyMatch(u -> u.getId().equals(subManager.getId()))) {
            throw new IllegalArgumentException("이미 서브 매니저로 등록된 사용자입니다.");
        }

        board.getBoardPolicy().getSubManagers().add(subManager);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));

        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /**
     * 서브 매니저 삭제
     */
    @Transactional
    public BoardPolicyDto removeSubManager(Long boardId, String subManagerNickname, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        User subManager = userValidator.findByNicknameOrThrow(subManagerNickname);

        boolean removed = board.getBoardPolicy().getSubManagers()
                .removeIf(user -> user.getId().equals(subManager.getId()));

        if (!removed) throw new IllegalArgumentException("해당 사용자는 서브 매니저가 아닙니다.");

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));

        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /**
     * 게시판 생성
     */
    @Transactional
    public BoardDto create(String topic, String description, Long currentUserId) {
        User boardManager = userValidator.findByIdOrThrow(currentUserId);
        Board board = Board.builder()
                .topic(topic).description(description).status(Board.Status.OPEN).build();
        board.setBoardPolicy(BoardPolicy.builder().board(board).boardManager(boardManager).likeThreshold(20).build());
        board.setDefaultCategories();

        boardRepository.save(board);

        eventPublisher.publishEvent(new BoardChangedEvent(board.getId()));

        return BoardDto.from(board);
    }

    /**
     * 게시판 상태 변경
     */
    @Transactional
    public BoardDto updateStatus(Long boardId, Board.Status newStatus, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.setStatus(newStatus);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));

        return BoardDto.from(board);
    }

    /**
     * 게시판 삭제
     */
    @Transactional
    public void delete(Long boardId, Long currentUserId) {
        Board board = boardValidator.getBoardIfBoardManager(boardId, currentUserId);
        board.setStatus(Board.Status.CLOSED);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));
    }
}