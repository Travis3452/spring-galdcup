package com.example.galdcup.user.role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRequestRepository extends JpaRepository<RoleRequest, Long> {
    Page<RoleRequest> findAllByStatus(RoleRequest.Status status, Pageable pageable);

    Page<RoleRequest> findAllByApplicantId(Long applicantId, Pageable pageable);

    boolean existsByApplicantIdAndStatus(Long applicantId, RoleRequest.Status status);
}