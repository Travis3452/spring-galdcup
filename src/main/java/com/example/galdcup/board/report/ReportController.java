package com.example.galdcup.board.report;
import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.common.rateLimit.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/boards/{boardId}") @PreAuthorize("isAuthenticated()")
public class ReportController {
 private final ReportService service;
 @PostMapping("/posts/{postId}/reports") @RateLimit(type=RateLimitType.INTERNAL)
 public void submit(@PathVariable Long boardId,@PathVariable Long postId,@AuthenticationPrincipal CustomUserDetails user,@Valid @RequestBody ReportData.Submit request){service.submit(boardId,postId,user.getId(),request.reason());}
 @GetMapping("/reports") public Page<ReportData.Summary> list(@PathVariable Long boardId,@AuthenticationPrincipal CustomUserDetails user,@RequestParam(defaultValue="OPEN") PostReport.Status status,@RequestParam(defaultValue="0") int page){return service.list(boardId,user.getId(),status,PageRequest.of(Math.max(0,page),20));}
 @GetMapping("/reports/{id}/submissions") public Page<ReportData.Submission> details(@PathVariable Long boardId,@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails user,@RequestParam(defaultValue="0") int page){return service.details(boardId,user.getId(),id,PageRequest.of(Math.max(0,page),20));}
 @PostMapping("/reports/{id}/handle") public void handle(@PathVariable Long boardId,@PathVariable Long id,@AuthenticationPrincipal CustomUserDetails user,@Valid @RequestBody ReportData.Handle request){service.handle(boardId,user.getId(),id,request);}
}
