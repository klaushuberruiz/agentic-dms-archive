package com.dms.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DmsException.class)
    public ResponseEntity<ErrorResponse> handleDmsException(DmsException ex, HttpServletRequest request) {
        String responseMessage = ex instanceof UnauthorizedAccessException
            ? "Access denied"
            : ex.getMessage();

        ErrorResponse response = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .message(responseMessage)
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
                fieldError -> fieldError.getDefaultMessage() == null ? "Validation failed" : fieldError.getDefaultMessage(),
                (first, second) -> first
            ));

        ErrorResponse response = ErrorResponse.builder()
            .errorCode("VALIDATION_FAILED")
            .message("Request validation failed")
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
            .message("An unexpected error occurred")
            .timestamp(Instant.now())
            .correlationId(extractCorrelationId(request))
            .build();

        return ResponseEntity.internalServerError().body(response);
    }

    private String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        return correlationId == null || correlationId.isBlank() ? "N/A" : correlationId;
    }
}
