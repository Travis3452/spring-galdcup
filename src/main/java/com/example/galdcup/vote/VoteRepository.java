package com.example.galdcup.vote;

import com.example.galdcup.voteSession.VoteSession;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface VoteRepository extends CrudRepository<Vote, Long> {
    Optional<Vote> findByVoteSessionAndVoterId(VoteSession voteSession, Long voterId);
}