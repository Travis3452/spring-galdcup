package com.example.galdcup.common.exception;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class ExceptionResponse {
    public static Map<String, Object> buildBody(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        return body;
    }
}