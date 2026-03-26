package com.example.galdcup.common.exception;

/**
 * 트래픽 제한(Rate Limit) 초과 시 발생하는 전용 예외
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}