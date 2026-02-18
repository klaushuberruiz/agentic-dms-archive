package com.dms.exception;

import org.springframework.http.HttpStatus;

public class LegalHoldActiveException extends DmsException {

    public LegalHoldActiveException(String message) {
        super("LEGAL_HOLD_ACTIVE", HttpStatus.CONFLICT, message);
    }
}
