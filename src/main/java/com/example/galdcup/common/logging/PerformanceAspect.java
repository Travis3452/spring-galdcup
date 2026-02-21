package com.example.galdcup.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    @Pointcut("execution(* com.example.galdcup..*Service.*(..))")
    private void serviceLayer() {}

    @Pointcut("execution(* com.example.galdcup.scheduler..*.*(..))")
    private void schedulerLayer() {}

    @Around("serviceLayer() || schedulerLayer()")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - start;
            log.info("[Performance] {} executed in {}ms",
                    joinPoint.getSignature().toShortString(), executionTime);
        }
    }
}
