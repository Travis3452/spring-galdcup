package com.example.galdcup.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 서비스 및 스케줄러 계층의 메서드 실행 시간 측정 및 성능 로깅
 */
@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    /** 컨트롤러 계층(Controller) 포인트컷 정의 */
    @Pointcut("execution(* com.example.galdcup..*Controller.*(..))")
    private void controllerLayer() {}

    /** 스케줄러 계층(Scheduler) 포인트컷 정의 */
    @Pointcut("execution(* com.example.galdcup..scheduler..*.*(..))")
    private void schedulerLayer() {}

    /** 대상 메서드 실행 전후 시간을 측정하여 로그 기록 */
    @Around("controllerLayer() || schedulerLayer()")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - start;
            log.info("[성능 측정] {} - 소요 시간: {}ms",
                    joinPoint.getSignature().toShortString(), executionTime);
        }
    }
}