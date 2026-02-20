package com.dms.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public class RateLimitExceededException extends DmsException {

    private final Instant retryAfter;

    public RateLimitExceededException(String message) {
        this(message, Instant.now().plusSeconds(60));
    }

    public RateLimitExceededException(String message, Instant retryAfter) {
        super("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfter = retryAfter;
    }

    public Instant getRetryAfter() {
        return retryAfter;
    }
}
