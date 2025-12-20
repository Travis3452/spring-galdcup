package com.example.galdcup.service;

import com.example.galdcup.dto.vote.VoteDto;
import com.example.galdcup.entity.User;
import com.example.galdcup.entity.Vote;
import com.example.galdcup.entity.VoteSession;
import com.example.galdcup.repository.UserRepository;
import com.example.galdcup.repository.VoteRepository;
import com.example.galdcup.repository.VoteSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteService {

    private final VoteRepository voteRepository;
    private final VoteSessionRepository voteSessionRepository;
    private final UserRepository userRepository;

    public VoteDto createVote(Long voteSessionId, Long userId, int selectedOptionIndex) {
        VoteSession session = voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new IllegalArgumentException("투표 세션을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (voteRepository.findByVoteSessionAndUser(session, user).isPresent()) {
            throw new IllegalStateException("이미 해당 게시판에 투표하였습니다.");
        }

        Vote vote = voteRepository.save(
                Vote.builder()
                        .voteSession(session)
                        .user(user)
                        .selectedOptionIndex(selectedOptionIndex)
                        .build()
        );

        return VoteDto.from(vote);
    }

    public long countVotes(Long voteSessionId, int selectedOptionIndex) {
        VoteSession session = voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new IllegalArgumentException("투표 세션을 찾을 수 없습니다."));
        return voteRepository.countByVoteSessionAndSelectedOptionIndex(session, selectedOptionIndex);
    }
}