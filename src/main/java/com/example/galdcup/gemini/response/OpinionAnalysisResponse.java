package com.example.galdcup.gemini.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini AI의 여론 분석 결과 (실시간 여론 지표)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpinionAnalysisResponse {

    private List<AnalysisResult> results;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResult {
        private String label;
        private Double supportRate;
    }

    /** 분석 실패 시 균등 배분 결과를 반환 */
    public static OpinionAnalysisResponse defaultResponse(List<String> candidates) {
        double equalRate = 100.0 / candidates.size();
        List<AnalysisResult> results = candidates.stream()
                .map(label -> new AnalysisResult(label, equalRate))
                .toList();
        return new OpinionAnalysisResponse(results);
    }
}