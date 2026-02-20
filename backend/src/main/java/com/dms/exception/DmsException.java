package com.dms.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class DmsException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected DmsException(String errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected DmsException(String errorCode, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
