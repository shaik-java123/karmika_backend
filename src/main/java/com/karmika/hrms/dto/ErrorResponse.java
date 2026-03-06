package com.karmika.hrms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response DTO for all API errors.
 * Provides consistent structure for frontend error handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Boolean success;
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    /**
     * Field-level validation errors.
     * Key: field name, Value: error message
     */
    private Map<String, String> validationErrors;

    /**
     * List of detailed error messages when multiple errors occur.
     */
    private List<String> details;

    /**
     * Circuit breaker state information (when applicable).
     */
    private String circuitBreakerState;

    // Convenience factory methods

    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse withValidation(int status, String error, String message,
            String path, Map<String, String> validationErrors) {
        return ErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();
    }

    public static ErrorResponse withCircuitBreaker(int status, String error, String message,
            String path, String circuitBreakerState) {
        return ErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .circuitBreakerState(circuitBreakerState)
                .build();
    }
}
