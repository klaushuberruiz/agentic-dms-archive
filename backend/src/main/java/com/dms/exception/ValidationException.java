package com.dms.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends DmsException {

    public ValidationException(String message) {
        super("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, message);
    }
}
