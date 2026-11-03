package com.example.galdcup.board.board;
import com.example.galdcup.board.board.domain.*;
import com.example.galdcup.board.board.event.BoardChangedEvent;
import com.example.galdcup.board.board.request.BoardRequest;
import com.example.galdcup.board.board.response.*;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
@Service @RequiredArgsConstructor
public class BoardPolicyService {
 private final BoardValidator boardValidator;
 private final UserValidator userValidator;
 private final BoardManagerRequestRepository boardManagerRequestRepository;
 private final ApplicationEventPublisher eventPublisher;
 private final BoardRepository boardRepository;
 private final com.example.galdcup.board.board.ban.BoardBanRepository bans;
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
    public BoardPolicyDto updatePolicy(Long boardId, BoardRequest.UpdatePolicy request, Long currentUserId) {
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


 @Transactional(readOnly=true)
 public org.springframework.data.domain.Page<com.example.galdcup.board.board.ban.BanData.View> listBans(Long boardId,Long actor,org.springframework.data.domain.Pageable page){
  boardValidator.requireBoardManagementAccess(boardId,actor);
  return bans.findByBoardIdOrderByCreatedAtDescIdDesc(boardId,page).map(com.example.galdcup.board.board.ban.BanData.View::from);
 }
 @Transactional
 public com.example.galdcup.board.board.ban.BanData.View ban(Long boardId,Long actor,com.example.galdcup.board.board.ban.BanData.Request request){
  boardRepository.lockForModeration(boardId).orElseThrow();
  Board board=boardValidator.requireBoardManagementAccess(boardId,actor);
  User target=userValidator.findByIdOrThrow(request.userId());
  if(board.getBoardPolicy().isAnyManager(target.getId())) throw new IllegalArgumentException("관리자는 해임 후 차단할 수 있습니다.");
  java.time.OffsetDateTime now=java.time.OffsetDateTime.now();
  if(!request.permanent() && request.expiresAt()==null) throw new IllegalArgumentException("차단 종료 시각을 지정해 주세요.");
  if(request.permanent() && request.expiresAt()!=null) throw new IllegalArgumentException("영구 차단에는 종료 시각을 지정하지 않습니다.");
  if(bans.countActive(boardId,target.getId(),now)>0) throw new IllegalStateException("이미 차단된 사용자입니다. 기존 차단을 해제한 후 다시 설정해 주세요.");
  return com.example.galdcup.board.board.ban.BanData.View.from(bans.save(com.example.galdcup.board.board.ban.BoardBan.create(board,target.getId(),target.getNickname(),request.reason().trim(),request.expiresAt(),actor,now)));
 }
 @Transactional
 public void revokeBan(Long boardId,Long actor,Long banId){
  boardRepository.lockForModeration(boardId).orElseThrow();
  boardValidator.requireBoardManagementAccess(boardId,actor);
  var ban=bans.findById(banId).orElseThrow(()->new IllegalArgumentException("차단 기록이 없습니다."));
  if(!ban.getBoard().getId().equals(boardId)) throw new org.springframework.security.access.AccessDeniedException("다른 게시판의 차단 기록입니다.");
  ban.revoke(actor,java.time.OffsetDateTime.now());
 }
}
