package com.example.galdcup.userAiAgent.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserAiAgentRepository extends JpaRepository<UserAiAgent, Long> {

    Optional<UserAiAgent> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
        SELECT a FROM UserAiAgent a 
        JOIN FETCH a.user 
        JOIN FETCH a.targetBoard 
        WHERE a.isActive = true AND a.expiredAt > :now
    """)
    List<UserAiAgent> findAllActiveAgentsWithTarget(@Param("now") OffsetDateTime now);

    List<UserAiAgent> findAllByExpiredAtBefore(OffsetDateTime now);
}