package com.example.galdcup.vote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE VoteOption o SET o.count = :totalCount WHERE o.id = :optionId")
    void updateVoteCount(@Param("optionId") Long optionId, @Param("totalCount") long totalCount);
}

