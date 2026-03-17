package com.example.galdcup.request.boardmanager;

import com.example.galdcup.board.Board;
import com.example.galdcup.board.BoardRepository;
import com.example.galdcup.request.boardmanager.dto.BoardManagerRequestDto;
import com.example.galdcup.user.User;
import com.example.galdcup.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardManagerRequestService {

    private final BoardManagerRequestRepository boardManagerRequestRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;

    @Transactional
    public BoardManagerRequestDto createBoardManagerRequest(Long applicantId, Long boardId) {
        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        boolean existsPending = boardManagerRequestRepository.existsByApplicantIdAndBoardIdAndStatus(
                applicantId, boardId, BoardManagerRequest.Status.WAITING);
        if (existsPending) {
            throw new IllegalStateException("이미 대기 중인 관리자 요청이 있습니다.");
        }

        BoardManagerRequest.Status status;
        if (board.getBoardPolicy().getBoardManager() == null) {
            status = BoardManagerRequest.Status.ACCEPTED;
            board.getBoardPolicy().setBoardManager(applicant);
            boardRepository.save(board);
        } else {
            status = BoardManagerRequest.Status.WAITING;
        }

        BoardManagerRequest request = BoardManagerRequest.builder()
                .applicant(applicant)
                .board(board)
                .status(status)
                .build();

        BoardManagerRequest saved = boardManagerRequestRepository.save(request);

        return BoardManagerRequestDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BoardManagerRequestDto> getPendingRequests(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판을 찾을 수 없습니다."));

        if (board.getBoardPolicy().getBoardManager() == null || !board.getBoardPolicy().getBoardManager().getId().equals(userId)) {
            throw new IllegalStateException("해당 게시판의 관리자만 요청을 조회할 수 있습니다.");
        }

        return boardManagerRequestRepository.findByBoardIdAndStatus(boardId, BoardManagerRequest.Status.WAITING)
                .stream()
                .map(BoardManagerRequestDto::from)
                .toList();
    }

    @Transactional
    public void approveRequest(Long requestId) {
        BoardManagerRequest request = boardManagerRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

        if (request.getStatus() != BoardManagerRequest.Status.WAITING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        request.setStatus(BoardManagerRequest.Status.ACCEPTED);

        Board board = request.getBoard();
        board.getBoardPolicy().setBoardManager(request.getApplicant());

        boardManagerRequestRepository.save(request);
        boardRepository.save(board);
    }

    @Transactional
    public void denyRequest(Long requestId) {
        BoardManagerRequest request = boardManagerRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

        if (request.getStatus() != BoardManagerRequest.Status.WAITING) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        request.setStatus(BoardManagerRequest.Status.DENIED);
        boardManagerRequestRepository.save(request);
    }
}