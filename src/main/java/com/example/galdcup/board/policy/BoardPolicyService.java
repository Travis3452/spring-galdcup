package com.example.galdcup.board.policy;
import com.example.galdcup.board.policy.response.BoardManagerRequestDto;
import com.example.galdcup.board.policy.response.BoardPolicyDto;
import com.example.galdcup.board.policy.domain.BoardManagerRequestRepository;
import com.example.galdcup.board.policy.domain.BoardManagerRequest;
import com.example.galdcup.board.board.domain.*;
import com.example.galdcup.board.board.event.BoardChangedEvent;
import com.example.galdcup.board.policy.request.BoardPolicyRequest;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
@Service @RequiredArgsConstructor
public class BoardPolicyService {
 private final BoardRepository boardRepository;
 private final BoardValidator boardValidator;
 private final UserValidator userValidator;
 private final BoardManagerRequestRepository boardManagerRequestRepository;
 private final ApplicationEventPublisher eventPublisher;
    /** 게시판 관리자 권한을 다른 유저에게 위임. */
    @Transactional
    public BoardPolicyDto delegateManager(Long boardId, String targetNickname, Long currentUserId) {
        Board board = boardValidator.requireBoardManagerAccess(boardId, currentUserId);
        User newManager = userValidator.findByNicknameOrThrow(targetNickname);

        board.getBoardPolicy().delegateMainManager(newManager);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));
        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** * 게시판 관리자 권한을 신청.
     * @implNote 소유자가 없는 게시판에 서브 매니저가 신청할 경우 즉시 승인 처리
     */
    @Transactional
    public BoardManagerRequestDto applyForManager(Long boardId, Long userId) {
        Board board = boardValidator.findBoardWithFullPolicyOrThrow(boardId);
        User applicant = userValidator.findByIdOrThrow(userId);

        boardValidator.validateNoPendingRequest(userId, boardId);

        BoardManagerRequest.Status status = BoardManagerRequest.Status.PENDING;

        if (board.getBoardPolicy().claimManagementIfEligible(applicant)) {
            status = BoardManagerRequest.Status.APPROVED;
            eventPublisher.publishEvent(new BoardChangedEvent(boardId));
        }

        BoardManagerRequest request = BoardManagerRequest.create(board, applicant, status);
        return BoardManagerRequestDto.from(boardManagerRequestRepository.save(request));
    }

    /** 게시판 정책을 수정. */
    @Transactional
    public BoardPolicyDto updatePolicy(Long boardId, BoardPolicyRequest.UpdatePolicy request, Long currentUserId) {
        Board board = boardValidator.requireBoardManagerAccess(boardId, currentUserId);
        board.getBoardPolicy().updateLikeThreshold(request.likeThreshold());

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));
        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** 게시판의 서브 매니저 권한 임명. */
    @Transactional
    public BoardPolicyDto addSubManager(Long boardId, String nickname, Long currentUserId) {
        Board board = boardValidator.requireBoardManagerAccess(boardId, currentUserId);
        User subManager = userValidator.findByNicknameOrThrow(nickname);

        board.getBoardPolicy().addSubManager(subManager);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));
        return BoardPolicyDto.from(board.getBoardPolicy());
    }

    /** 게시판의 서브 매니저 권한 제거. */
    @Transactional
    public BoardPolicyDto removeSubManager(Long boardId, String nickname, Long currentUserId) {
        Board board = boardValidator.requireBoardManagerAccess(boardId, currentUserId);
        User subManager = userValidator.findByNicknameOrThrow(nickname);

        board.getBoardPolicy().removeSubManager(subManager);

        eventPublisher.publishEvent(new BoardChangedEvent(boardId));
        return BoardPolicyDto.from(board.getBoardPolicy());
    }


    /** 회원 탈퇴 시 기존 메인 관리자 관계를 해제합니다. */
    @Transactional
    public void releaseManagerOnWithdrawal(Long userId) {
        boardRepository.removeBoardManagerByUserId(userId);
    }

}
