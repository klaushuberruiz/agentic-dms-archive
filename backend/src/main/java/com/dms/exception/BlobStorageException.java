package com.dms.exception;

import org.springframework.http.HttpStatus;

public class BlobStorageException extends DmsException {

    public BlobStorageException(String message) {
        super("STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public BlobStorageException(String message, Throwable cause) {
        super("STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
