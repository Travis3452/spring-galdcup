package com.example.galdcup.OpinionAnalysis;

import com.example.galdcup.comment.domain.Comment;
import com.example.galdcup.comment.domain.CommentRepository;
import com.example.galdcup.gemini.GeminiService;
import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import com.example.galdcup.vote.domain.VoteOption;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.redis.VoteSessionRedisManager;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpinionAnalysisService {

    private final VoteSessionValidator voteSessionValidator;
    private final CommentRepository commentRepository;
    private final GeminiService geminiService;
    private final VoteSessionRedisManager voteSessionRedisManager;

    /**
     * 캐싱된 분석 결과 조회
     */
    public OpinionAnalysisResponse getCachedAnalysis(Long sessionId) {
        return voteSessionRedisManager.getOpinionAnalysis(sessionId).orElse(null);
    }

    /**
     * 실시간 여론 분석 수행 및 결과 캐싱
     */
    public OpinionAnalysisResponse performAnalysisAndCache(Long sessionId) {
        VoteSession session = voteSessionValidator.validateAndGetVoteSessionWithOptionsAndBoard(sessionId);

        // 1. 후보자 리스트 추출
        List<String> candidateLabels = session.getOptions().stream()
                .map(VoteOption::getLabel)
                .toList();

        // 2. DB에서 유저별 최신 댓글 50개 로드
        Pageable top50 = PageRequest.of(0, 50);
        List<Comment> rawComments = commentRepository.findTopUniqueUsersByBoardId(
                session.getBoard().getId(),
                top50
        );

        // 3. 토큰 최적화
        String optimizedComments = optimizeComments(rawComments);

        // 4. Gemini 분석 요청
        OpinionAnalysisResponse response = geminiService.analyzeOpinion(
                session.getTopic(),
                session.getDescription(),
                candidateLabels,
                optimizedComments
        );

        // 5. Redis 캐시 저장
        voteSessionRedisManager.saveOpinionAnalysis(sessionId, response);

        return response;
    }

    /**
     * 텍스트 최적화 (1,500자 제한)
     */
    private String optimizeComments(List<Comment> comments) {
        StringBuilder sb = new StringBuilder();
        int MAX_CHARS = 1500;

        for (Comment comment : comments) {
            String content = comment.getContent();
            if (sb.length() + content.length() + 3 > MAX_CHARS) break;
            sb.append("- ").append(content).append("\n");
        }

        return sb.toString();
    }
}