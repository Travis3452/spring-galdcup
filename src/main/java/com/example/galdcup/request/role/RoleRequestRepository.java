package com.example.galdcup.request.role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRequestRepository extends JpaRepository<RoleRequest, Long> {
    List<RoleRequest> findByStatus(RoleRequest.Status status);

    boolean existsByApplicantIdAndStatus(Long applicantId, RoleRequest.Status status);
}