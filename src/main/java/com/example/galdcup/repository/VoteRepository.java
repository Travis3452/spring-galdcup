package com.example.galdcup.repository;

import com.example.galdcup.entity.Vote;
import com.example.galdcup.entity.VoteSession;
import com.example.galdcup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByVoteSessionAndUser(VoteSession voteSession, User user);
}