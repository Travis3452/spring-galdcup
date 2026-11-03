package com.example.galdcup.board.report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
public interface ReportSubmissionRepository extends JpaRepository<ReportSubmission,Long>{
 boolean existsByReportIdAndReporterId(Long reportId,Long reporterId);
 Page<ReportSubmission> findByReportIdOrderByCreatedAtDescIdDesc(Long reportId,Pageable page);
}
