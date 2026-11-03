package com.example.galdcup.board.report;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="report_submissions",uniqueConstraints=@UniqueConstraint(name="uk_report_submitter",columnNames={"report_id","reporter_id"}))
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ReportSubmission {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="report_id",nullable=false) private PostReport report;
 @Column(name="reporter_id",nullable=false) private Long reporterId;
 @Column(nullable=false,length=500) private String reason;
 @Column(nullable=false) private OffsetDateTime createdAt;
 public static ReportSubmission create(PostReport report,Long user,String reason){ReportSubmission s=new ReportSubmission();s.report=report;s.reporterId=user;s.reason=reason;s.createdAt=OffsetDateTime.now();return s;}
}
