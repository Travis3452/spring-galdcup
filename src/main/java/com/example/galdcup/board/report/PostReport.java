package com.example.galdcup.board.report;
import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.board.post.domain.Post;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Table(name="post_reports",uniqueConstraints=@UniqueConstraint(name="uk_report_post",columnNames={"board_id","post_id"}))
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class PostReport {
 public enum Status { OPEN, RESOLVED, DISMISSED }
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="board_id",nullable=false) private Board board;
 @Column(name="post_id",nullable=false) private Long postId;
 @Column(nullable=false,length=100) private String title;
 private Long authorId;
 @Column(nullable=false,length=50) private String authorNickname;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
 @Column(nullable=false) private long reportCount;
 @Column(nullable=false) private OffsetDateTime createdAt;
 @Column(nullable=false) private OffsetDateTime lastReportedAt;
 private OffsetDateTime handledAt;
 private Long handledBy;
 @Column(length=500) private String resolution;
 public static PostReport create(Post p){PostReport r=new PostReport();r.board=p.getBoard();r.postId=p.getId();r.title=p.getTitle();r.authorId=p.getAuthor().getId();r.authorNickname=p.getAuthor().getNickname();r.status=Status.OPEN;r.createdAt=OffsetDateTime.now();r.lastReportedAt=r.createdAt;return r;}
 public void recordSubmission(){reportCount++;lastReportedAt=OffsetDateTime.now();status=Status.OPEN;handledAt=null;handledBy=null;resolution=null;}
 public void handle(Status status,Long actor,String resolution){this.status=status;handledBy=actor;handledAt=OffsetDateTime.now();this.resolution=resolution;}
}
