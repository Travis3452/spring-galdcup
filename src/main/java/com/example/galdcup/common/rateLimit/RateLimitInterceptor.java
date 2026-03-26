package com.example.galdcup.common.rateLimit;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.example.galdcup.common.exception.RateLimitExceededException;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ProxyManager<String> proxyManager;

    /** Redis 키 구분자 */
    private static final String L1_IP_KEY_PREFIX = "galdcup:rl:l1:ip:";
    private static final String L2_USER_KEY_PREFIX = "galdcup:rl:l2:";

    /** L1: 모든 IP 공통 (분당 60회) */
    private final Supplier<BucketConfiguration> l1Config = () -> BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(200).refillGreedy(100, Duration.ofMinutes(1))).build();

    /** L2-INTERNAL: DB 쓰기 등 내부 리소스 사용 (분당 15회) */
    private final Supplier<BucketConfiguration> internalConfig = () -> BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(15).refillGreedy(15, Duration.ofMinutes(1))).build();

    /** L2-EXTERNAL: Gemini API 등 외부 API 사용 (분당 3회) */
    private final Supplier<BucketConfiguration> externalConfig = () -> BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofMinutes(1))).build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 전역 L1(IP) 가드 실행
        String clientIp = getClientIp(request);
        applyLimit(L1_IP_KEY_PREFIX + clientIp, l1Config, "요청이 너무 빈번합니다. 잠시 후 다시 시도해주세요.");

        // 어노테이션 기반 L2(User) 가드 선택적 실행
        if (handler instanceof HandlerMethod handlerMethod) {
            RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (annotation != null) {
                applyL2Limit(annotation.type());
            }
        }

        return true;
    }

    /** L2 유저별 분기 제한 로직 */
    private void applyL2Limit(RateLimitType type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않은 사용자가 @RateLimit API 접근 시 차단
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new RateLimitExceededException("로그인이 필요한 서비스입니다.");
        }

        String userId = auth.getName();
        String key = L2_USER_KEY_PREFIX + type.getPrefix() + ":" + userId;
        Supplier<BucketConfiguration> config = (type == RateLimitType.EXTERNAL) ? externalConfig : internalConfig;

        applyLimit(key, config, "요청이 너무 빈번합니다. 잠시 후 다시 시도해주세요.");
    }

    /** 공통 토큰 소모 로직 */
    private void applyLimit(String key, Supplier<BucketConfiguration> config, String errorMsg) {
        boolean canProceed = proxyManager.builder().build(key, config).tryConsume(1);
        if (!canProceed) {
            throw new RateLimitExceededException(errorMsg);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}