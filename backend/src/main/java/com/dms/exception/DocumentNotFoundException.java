package com.dms.exception;

import org.springframework.http.HttpStatus;

public class DocumentNotFoundException extends DmsException {
    public DocumentNotFoundException(String message) {
        super("DOCUMENT_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
