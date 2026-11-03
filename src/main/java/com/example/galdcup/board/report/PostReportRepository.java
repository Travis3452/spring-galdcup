package com.example.galdcup.board.report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface PostReportRepository extends JpaRepository<PostReport,Long>{
 Optional<PostReport> findByBoardIdAndPostId(Long boardId,Long postId);
 Page<PostReport> findByBoardIdAndStatusOrderByLastReportedAtDescIdDesc(Long boardId,PostReport.Status status,Pageable page);
}
