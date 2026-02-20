package com.dms.exception;

import org.springframework.http.HttpStatus;

public class BlobStorageException extends DmsException {

    public BlobStorageException(String message) {
        super("STORAGE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
