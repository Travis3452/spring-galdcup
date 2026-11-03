package com.example.galdcup.board.report;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
public final class ReportData {
 public record Submit(@NotBlank @Size(max=500) String reason){}
 public enum Action { DISMISS, RESOLVE, DELETE, BAN, DELETE_AND_BAN }
 public record Handle(@NotNull Action action,@NotBlank @Size(max=500) String reason,boolean permanent,OffsetDateTime expiresAt){}
 public record Summary(Long id,Long postId,String title,Long authorId,String authorNickname,PostReport.Status status,long reportCount,OffsetDateTime lastReportedAt,Long handledBy,OffsetDateTime handledAt,String resolution){
  public static Summary from(PostReport r){return new Summary(r.getId(),r.getPostId(),r.getTitle(),r.getAuthorId(),r.getAuthorNickname(),r.getStatus(),r.getReportCount(),r.getLastReportedAt(),r.getHandledBy(),r.getHandledAt(),r.getResolution());}
 }
 public record Submission(Long id,String reason,OffsetDateTime createdAt){public static Submission from(ReportSubmission s){return new Submission(s.getId(),s.getReason(),s.getCreatedAt());}}
}
