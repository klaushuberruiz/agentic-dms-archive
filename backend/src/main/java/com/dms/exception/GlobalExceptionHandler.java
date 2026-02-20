package com.dms.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
        RateLimitExceededException ex,
        HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .message(resolveMessageForCode(ex.getErrorCode(), ex.getMessage()))
            .timestamp(Instant.now())
            .correlationId(extractCorrelationId(request))
            .build();

        return ResponseEntity.status(ex.getHttpStatus())
            .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfter().getEpochSecond()))
            .body(response);
    }

    @ExceptionHandler(DmsException.class)
    public ResponseEntity<ErrorResponse> handleDmsException(DmsException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .message(resolveMessageForCode(ex.getErrorCode(), ex.getMessage()))
            .timestamp(Instant.now())
            .correlationId(extractCorrelationId(request))
            .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                this::resolveFieldError,
                (first, second) -> first
            ));

        ErrorResponse response = ErrorResponse.builder()
            .errorCode("VALIDATION_FAILED")
            .message(resolveMessageForCode("VALIDATION_FAILED", "Request validation failed"))
            .timestamp(Instant.now())
            .correlationId(extractCorrelationId(request))
            .fieldErrors(fieldErrors)
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
            .errorCode("INTERNAL_SERVER_ERROR")
            .message(resolveMessageForCode("INTERNAL_SERVER_ERROR", "An unexpected error occurred"))
            .timestamp(Instant.now())
            .correlationId(extractCorrelationId(request))
            .build();

        return ResponseEntity.internalServerError().body(response);
    }

    private String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        return correlationId == null || correlationId.isBlank() ? "N/A" : correlationId;
    }

    private String resolveMessageForCode(String errorCode, String fallback) {
        Locale locale = LocaleContextHolder.getLocale();
        String messageKey = "error." + errorCode.toLowerCase(Locale.ROOT);
        return messageSource.getMessage(messageKey, null, fallback, locale);
    }

    private String resolveFieldError(FieldError fieldError) {
        Locale locale = LocaleContextHolder.getLocale();
        String fallback = fieldError.getDefaultMessage() == null
            ? resolveMessageForCode("VALIDATION_FAILED", "Validation failed")
            : fieldError.getDefaultMessage();
        try {
            return messageSource.getMessage(fieldError, locale);
        } catch (NoSuchMessageException ignored) {
            return fallback;
        }
    }
}
