package com.example.galdcup.userAiAgent.domain;

import com.example.galdcup.board.board.domain.Board;
import com.example.galdcup.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 유저 맞춤형 AI 용병 엔티티
 */
@Entity
@Table(name = "user_ai_agent")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAiAgent {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_board_id", nullable = false)
    private Board targetBoard;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_mode", nullable = false, length = 20)
    private AgentMode agentMode;

    @Column(name = "encrypted_api_key", nullable = false, length = 512)
    private String encryptedApiKey;

    @Column(name = "persona_prompt", nullable = false, length = 500)
    private String personaPrompt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "interval_minutes", nullable = false)
    private int intervalMinutes;

    @Column(name = "last_executed_at", nullable = false)
    private OffsetDateTime lastExecutedAt;

    @Column(name = "expired_at", nullable = false)
    private OffsetDateTime expiredAt;

    /**
     * AI 용병 생성 정적 팩토리 메서드
     */
    public static UserAiAgent create(User user, Board targetBoard, AgentMode agentMode, String encryptedApiKey, String personaPrompt) {
        OffsetDateTime now = OffsetDateTime.now(KST_ZONE);
        return UserAiAgent.builder()
                .user(user)
                .targetBoard(targetBoard)
                .agentMode(agentMode)
                .encryptedApiKey(encryptedApiKey)
                .personaPrompt(personaPrompt)
                .isActive(true)
                .intervalMinutes(30)
                .lastExecutedAt(now)
                .expiredAt(now.plusHours(24))
                .build();
    }

    /**
     * 24시간 만료 여부 확인
     */
    public boolean isExpired() {
        return OffsetDateTime.now(KST_ZONE).isAfter(this.expiredAt);
    }

    /**
     * 활동 실행 가능 여부 검증
     */
    public boolean isReadyToExecute() {
        if (!this.isActive || isExpired()) {
            return false;
        }
        return OffsetDateTime.now(KST_ZONE).isAfter(this.lastExecutedAt.plusMinutes(this.intervalMinutes));
    }

    /**
     * 마지막 실행 시각 갱신
     */
    public void updateLastExecutedAt() {
        this.lastExecutedAt = OffsetDateTime.now(KST_ZONE);
    }

    /**
     * 용병 설정 변경
     */
    public void updateSetting(String personaPrompt, Board targetBoard, AgentMode agentMode, Integer intervalMinutes) {
        if (personaPrompt != null && !personaPrompt.isBlank()) {
            this.personaPrompt = personaPrompt;
        }
        if (targetBoard != null) {
            this.targetBoard = targetBoard;
        }
        if (agentMode != null) {
            this.agentMode = agentMode;
        }
        if (intervalMinutes != null && intervalMinutes > 0) {
            this.intervalMinutes = intervalMinutes;
        }
    }

    /**
     * 활성화 상태 변경
     */
    public void toggleActive(boolean isActive) {
        this.isActive = isActive;
    }
}