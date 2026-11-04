package com.example.galdcup.board;
import com.example.galdcup.board.policy.domain.BoardPolicy;
import com.example.galdcup.board.board.*;
import com.example.galdcup.board.board.domain.*;
import com.example.galdcup.board.board.validator.BoardValidator;
import com.example.galdcup.board.ban.*;
import com.example.galdcup.board.report.*;
import com.example.galdcup.board.post.PostService;
import com.example.galdcup.board.post.domain.*;
import com.example.galdcup.user.domain.User;
import com.example.galdcup.user.validator.UserValidator;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.PageRequest;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ModerationTests {
 final Board board=Board.builder().id(1L).build();
 final User manager=User.builder().id(10L).nickname("manager").build();
 final User author=User.builder().id(20L).nickname("author").build();
 final BoardRepository boards=mock(BoardRepository.class);
 final BoardValidator access=mock(BoardValidator.class);
 final PostRepository posts=mock(PostRepository.class);
 final PostReportRepository reports=mock(PostReportRepository.class);
 final ReportSubmissionRepository submissions=mock(ReportSubmissionRepository.class);
 final UserValidator users=mock(UserValidator.class);
 final BoardBanService policy=mock(BoardBanService.class);
 final PostService postService=mock(PostService.class);
 final ReportService service=new ReportService(boards,access,posts,reports,submissions,users,policy,postService);
 PostReport report;
 @BeforeEach void setup(){
  board.assignPolicy(BoardPolicy.create(board,manager));
  Post post=Post.builder().id(5L).board(board).title("title").author(com.example.galdcup.board.post.domain.embedded.Author.from(author)).build();
  report=PostReport.create(post);
  when(boards.lockForModeration(1L)).thenReturn(Optional.of(board));
  when(posts.findById(5L)).thenReturn(Optional.of(post));
  when(reports.findByBoardIdAndPostId(1L,5L)).thenReturn(Optional.of(report));
  when(reports.findById(7L)).thenReturn(Optional.of(report));
 }
 @Test void differentUsersAreGroupedAndDuplicateSubmitterIsRejected(){
  service.submit(1L,5L,30L,"reason");service.submit(1L,5L,31L,"reason2");
  assertEquals(2,report.getReportCount());verify(submissions,times(2)).save(any());
  when(submissions.existsByReportIdAndReporterId(null,30L)).thenReturn(true);
  assertThrows(IllegalStateException.class,()->service.submit(1L,5L,30L,"again"));assertEquals(2,report.getReportCount());
 }
 @Test void foreignPostCannotBeReported(){when(boards.lockForModeration(2L)).thenReturn(Optional.of(board));assertThrows(AccessDeniedException.class,()->service.submit(2L,5L,30L,"reason"));}
 @Test void unauthorizedUserCannotReadReports(){doThrow(new AccessDeniedException("denied")).when(access).requireBoardManagementAccess(1L,30L);assertThrows(AccessDeniedException.class,()->service.list(1L,30L,PostReport.Status.OPEN,PageRequest.of(0,20)));verifyNoInteractions(reports);}
 @Test void foreignReportCannotBeHandled(){when(boards.lockForModeration(2L)).thenReturn(Optional.of(board));assertThrows(AccessDeniedException.class,()->service.handle(2L,10L,7L,new ReportData.Handle(ReportData.Action.DELETE,"reason",false,null)));verifyNoInteractions(postService);}
 @Test void deleteAndBanRecordsResolution(){when(posts.existsById(5L)).thenReturn(true);service.handle(1L,10L,7L,new ReportData.Handle(ReportData.Action.DELETE_AND_BAN,"reason",true,null));verify(policy).ban(eq(1L),eq(10L),any());verify(postService).deleteForBoardManager(5L,1L,10L);assertEquals(PostReport.Status.RESOLVED,report.getStatus());assertEquals(10L,report.getHandledBy());}
 @Test void dismissDoesNotDeleteOrBan(){service.handle(1L,10L,7L,new ReportData.Handle(ReportData.Action.DISMISS,"reason",false,null));verifyNoInteractions(policy,postService);assertEquals(PostReport.Status.DISMISSED,report.getStatus());}
 @Test void handledReportRejectsRepeatedAction(){report.handle(PostReport.Status.RESOLVED,10L,"done");assertThrows(IllegalStateException.class,()->service.handle(1L,10L,7L,new ReportData.Handle(ReportData.Action.DELETE,"reason",false,null)));verifyNoInteractions(postService);}
 @Test void newReporterReopensHandledCase(){report.handle(PostReport.Status.DISMISSED,10L,"done");report.recordSubmission();assertEquals(PostReport.Status.OPEN,report.getStatus());assertNull(report.getHandledAt());}
 @Test void timedBanExpiresAtExactBoundaryAndPermanentBanCanBeRevoked(){var now=OffsetDateTime.now();var ban=BoardBan.create(board,20L,"author","reason",now.plusHours(1),10L,now);assertTrue(ban.isActiveAt(now));assertFalse(ban.isActiveAt(now.plusHours(1)));var permanent=BoardBan.create(board,20L,"author","reason",null,10L,now);assertTrue(permanent.isActiveAt(now.plusYears(10)));permanent.revoke(10L,now);assertFalse(permanent.isActiveAt(now));assertThrows(IllegalArgumentException.class,()->BoardBan.create(board,20L,"author","reason",now,10L,now));}
 @Test void blockedWriterIsDeniedButOtherBoardsAreAllowed(){var bans=mock(BoardBanRepository.class);var guard=new BoardWriteGuard(bans);when(bans.findActive(eq(1L),eq(20L),any(),any())).thenReturn(List.of(BoardBan.create(board,20L,"author","reason",null,10L,OffsetDateTime.now())));assertThrows(AccessDeniedException.class,()->guard.requireAllowed(1L,20L));assertDoesNotThrow(()->guard.requireAllowed(2L,20L));}
 @Test void vacancyClaimAndManagementUsePolicy(){var p=BoardPolicy.create(board,null);p.addSubManager(manager);assertFalse(p.claimManagementIfEligible(author));assertTrue(p.claimManagementIfEligible(manager));assertTrue(p.isBoardManager(10L));assertFalse(p.claimManagementIfEligible(author));}
}
