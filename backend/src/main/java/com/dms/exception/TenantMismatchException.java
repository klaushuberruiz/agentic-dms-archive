package com.dms.exception;

import org.springframework.http.HttpStatus;

public class TenantMismatchException extends DmsException {

    public TenantMismatchException(String message) {
        super("TENANT_MISMATCH", HttpStatus.FORBIDDEN, message);
    }
}
