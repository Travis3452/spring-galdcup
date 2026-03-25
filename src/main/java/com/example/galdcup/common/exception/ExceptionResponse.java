package com.example.galdcup.common.exception;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 일관된 에러 메시지 전달을 위한 API 예외 응답 바디
 */
public class ExceptionResponse {

    /**
     * HTTP 상태 코드, 에러 유형, 상세 메시지를 포함한 공통 응답 바디 구성
     */
    public static Map<String, Object> buildBody(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();

        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", OffsetDateTime.now(ZoneId.of("Asia/Seoul")));

        return body;
    }
}