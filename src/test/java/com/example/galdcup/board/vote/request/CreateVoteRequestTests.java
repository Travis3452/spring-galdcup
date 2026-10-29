package com.example.galdcup.board.vote.request;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateVoteRequestTests {

    @Test
    void missingOptionIsRejectedInsteadOfDefaultingToFirstOption() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new CreateVoteRequest(null, null)).isEmpty());
        }
    }

    @Test
    void negativeOptionIsRejected() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new CreateVoteRequest(null, -1)).isEmpty());
        }
    }

    @Test
    void firstOptionIsValid() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(new CreateVoteRequest(null, 0)).isEmpty());
        }
    }
}
