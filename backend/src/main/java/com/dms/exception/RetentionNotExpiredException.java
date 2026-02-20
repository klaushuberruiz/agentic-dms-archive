package com.dms.exception;

import org.springframework.http.HttpStatus;

public class RetentionNotExpiredException extends DmsException {

    public RetentionNotExpiredException(String message) {
        super("RETENTION_NOT_EXPIRED", HttpStatus.CONFLICT, message);
    }
}
