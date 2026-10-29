package com.example.galdcup.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTests {

    @Test
    void invalidRefreshCredentialsReturn401WithoutInternalDetails() {
        var response = new GlobalExceptionHandler().handleAuthentication(
                new BadCredentialsException("internal token validation details"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().get("status"));
        assertEquals("UNAUTHORIZED", response.getBody().get("error"));
        assertEquals("인증 정보가 유효하지 않습니다.", response.getBody().get("message"));
    }
}
