package com.example.galdcup.board.report;
import com.example.galdcup.board.board.domain.BoardRepository;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.board.board.BoardPolicyService;
import com.example.galdcup.board.board.ban.BanData;
import com.example.galdcup.board.post.PostService;
import com.example.galdcup.board.post.domain.PostRepository;
import com.example.galdcup.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ReportService {
 private final BoardRepository boards; private final BoardValidator access;
 private final PostRepository posts; private final PostReportRepository reports;
 private final ReportSubmissionRepository submissions; private final UserValidator users;
 private final BoardPolicyService policy; private final PostService postService;
 @Transactional public void submit(Long board,Long post,Long user,String reason){
  boards.lockForModeration(board).orElseThrow();users.findByIdOrThrow(user);
  var p=posts.findById(post).orElseThrow(()->new IllegalArgumentException("게시글이 없습니다."));
  if(!p.getBoard().getId().equals(board))throw new AccessDeniedException("다른 게시판의 게시글입니다.");
  var report=reports.findByBoardIdAndPostId(board,post).orElseGet(()->reports.save(PostReport.create(p)));
  if(submissions.existsByReportIdAndReporterId(report.getId(),user))throw new IllegalStateException("이미 신고한 게시글입니다.");
  submissions.save(ReportSubmission.create(report,user,reason.trim()));report.recordSubmission();
 }
 public Page<ReportData.Summary> list(Long board,Long actor,PostReport.Status status,Pageable page){access.requireBoardManagementAccess(board,actor);return reports.findByBoardIdAndStatusOrderByLastReportedAtDescIdDesc(board,status,page).map(ReportData.Summary::from);}
 public Page<ReportData.Submission> details(Long board,Long actor,Long id,Pageable page){requireReport(board,actor,id);return submissions.findByReportIdOrderByCreatedAtDescIdDesc(id,page).map(ReportData.Submission::from);}
 private PostReport requireReport(Long board,Long actor,Long id){access.requireBoardManagementAccess(board,actor);var r=reports.findById(id).orElseThrow(()->new IllegalArgumentException("신고가 없습니다."));if(!r.getBoard().getId().equals(board))throw new AccessDeniedException("다른 게시판의 신고입니다.");return r;}
 @Transactional public void handle(Long board,Long actor,Long id,ReportData.Handle request){
  boards.lockForModeration(board).orElseThrow();var r=requireReport(board,actor,id);
  if(r.getStatus()!=PostReport.Status.OPEN)throw new IllegalStateException("이미 처리된 신고입니다.");
  var action=request.action();
  if(action==ReportData.Action.BAN || action==ReportData.Action.DELETE_AND_BAN){
   if(r.getAuthorId()==null)throw new IllegalArgumentException("작성자를 차단할 수 없습니다.");
   policy.ban(board,actor,new BanData.Request(r.getAuthorId(),request.reason(),request.permanent(),request.expiresAt()));
  }
  if(action==ReportData.Action.DELETE || action==ReportData.Action.DELETE_AND_BAN){
   if(posts.existsById(r.getPostId()))postService.deleteForBoardManager(r.getPostId(),board,actor);
  }
  r.handle(action==ReportData.Action.DISMISS?PostReport.Status.DISMISSED:PostReport.Status.RESOLVED,actor,request.reason().trim());
 }
}
