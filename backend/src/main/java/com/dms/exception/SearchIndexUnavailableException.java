package com.dms.exception;

import org.springframework.http.HttpStatus;

public class SearchIndexUnavailableException extends DmsException {

    public SearchIndexUnavailableException(String message) {
        super("SEARCH_INDEX_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
