package com.dms.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends DmsException {

    public RateLimitExceededException(String message) {
        super("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
