package com.example.galdcup.board.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardManagerRequestRepository extends JpaRepository<BoardManagerRequest, Long> {

    boolean existsByApplicantIdAndBoardIdAndStatus(Long applicantId, Long boardId, BoardManagerRequest.Status status);

    List<BoardManagerRequest> findByBoardIdAndStatus(Long boardId, BoardManagerRequest.Status status);
}
