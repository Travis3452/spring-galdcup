package com.example.galdcup.opinionAnalysis;

import com.example.galdcup.comment.domain.Comment;
import com.example.galdcup.comment.domain.CommentRepository;
import com.example.galdcup.gemini.GeminiService;
import com.example.galdcup.gemini.response.OpinionAnalysisResponse;
import com.example.galdcup.vote.domain.VoteOption;
import com.example.galdcup.voteSession.domain.VoteSession;
import com.example.galdcup.voteSession.validator.VoteSessionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpinionAnalysisService {

    private final VoteSessionValidator voteSessionValidator;
    private final CommentRepository commentRepository;
    private final GeminiService geminiService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_FORMAT = "galdcup:vote:session:%d:analysis";

    /**
     * 캐싱된 분석 결과 조회
     */
    public OpinionAnalysisResponse getCachedAnalysis(Long sessionId) {
        String cacheKey = String.format(CACHE_KEY_FORMAT, sessionId);
        return (OpinionAnalysisResponse) redisTemplate.opsForValue().get(cacheKey);
    }

    /**
     * 실시간 여론 분석 수행 및 결과 캐싱
     */
    @Transactional(readOnly = true)
    public OpinionAnalysisResponse performAnalysisAndCache(Long sessionId) {

        VoteSession session = voteSessionValidator.validateAndGetVoteSession(sessionId);

        // 1. 후보자 리스트 추출
        List<String> candidateLabels = session.getOptions().stream()
                .map(VoteOption::getLabel)
                .toList();

        // 2. DB에서 유저별 최신 댓글 500개 로드
        Pageable top500 = PageRequest.of(0, 500);
        List<Comment> rawComments = commentRepository.findTopUniqueUsersByBoardId(
                session.getBoard().getId(),
                top500
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
        saveToCache(sessionId, response);

        return response;
    }

    /**
     * Redis 캐시 저장 헬퍼
     */
    private void saveToCache(Long sessionId, OpinionAnalysisResponse response) {
        String cacheKey = String.format(CACHE_KEY_FORMAT, sessionId);
        try {
            redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(30));
        } catch (Exception e) {
            log.error("Redis 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 텍스트 최적화 (3,000자 제한)
     */
    private String optimizeComments(List<Comment> comments) {
        StringBuilder sb = new StringBuilder();
        int MAX_CHARS = 3000;

        for (Comment comment : comments) {
            String content = comment.getContent();
            if (sb.length() + content.length() + 3 > MAX_CHARS) break;
            sb.append("- ").append(content).append("\n");
        }

        return sb.toString();
    }
}