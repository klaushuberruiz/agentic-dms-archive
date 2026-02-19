package com.dms.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends DmsException {

    public UnauthorizedAccessException(String message) {
        super("UNAUTHORIZED_ACCESS", HttpStatus.FORBIDDEN, message);
    }
}
