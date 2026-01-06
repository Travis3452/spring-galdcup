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

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteService {

    private final VoteRepository voteRepository;
    private final VoteSessionRepository voteSessionRepository;
    private final UserRepository userRepository;

    /**
     * 투표 생성
     */
    public VoteDto createVote(Long voteSessionId, Long userId, int selectedOptionIndex) {
        VoteSession session = voteSessionRepository.findById(voteSessionId)
                .orElseThrow(() -> new IllegalArgumentException("투표 세션을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 중복 투표 방지
        if (voteRepository.findByVoteSessionAndUser(session, user).isPresent()) {
            throw new IllegalStateException("이미 해당 게시판에 투표하였습니다.");
        }

        // 투표 시간 검증
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime())) {
            throw new IllegalStateException("현재는 투표 가능 시간이 아닙니다.");
        }

        // 옵션 인덱스 검증
        if (selectedOptionIndex < 0 || selectedOptionIndex >= session.getOptions().size()) {
            throw new IllegalArgumentException("잘못된 투표 옵션입니다.");
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
}