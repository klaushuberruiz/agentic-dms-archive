package com.dms.exception;

import org.springframework.http.HttpStatus;

public class ConcurrentModificationException extends DmsException {

    public ConcurrentModificationException(String message) {
        super("CONCURRENT_MODIFICATION", HttpStatus.CONFLICT, message);
    }
}
