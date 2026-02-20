package com.dms.exception;

import org.springframework.http.HttpStatus;

public class EmbeddingGenerationException extends DmsException {

    public EmbeddingGenerationException(String message) {
        super("EMBEDDING_GENERATION_FAILED", HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public EmbeddingGenerationException(String message, Throwable cause) {
        super("EMBEDDING_GENERATION_FAILED", HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
