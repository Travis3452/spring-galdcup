package com.example.galdcup.vote;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface VoteRepository extends CrudRepository<Vote, Long> {
    Optional<Vote> findByVoteSessionAndVoterId(VoteSession voteSession, Long voterId);
}