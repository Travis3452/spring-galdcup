package com.example.galdcup.common.rateLimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 특정 API에 대해 L2 트래픽 제한을 적용하기 위한 커스텀 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    RateLimitType type() default RateLimitType.INTERNAL;
}