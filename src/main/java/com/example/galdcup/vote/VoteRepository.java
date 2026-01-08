package com.example.galdcup.vote;

import com.example.galdcup.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByVoteSessionAndUser(VoteSession voteSession, User user);
}