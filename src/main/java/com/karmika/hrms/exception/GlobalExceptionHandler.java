package com.karmika.hrms.exception;

import com.karmika.hrms.dto.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire Karmika HRMS application.
 * Catches all exceptions thrown across controllers and services,
 * returning consistent, structured error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── Custom Business Exceptions ──────────────────────────────────────────

    /**
     * Handle resource not found (404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Handle bad request (400)
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Handle duplicate resource conflict (409)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "Conflict",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Handle unauthorized access (401)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Handle service unavailable (503) — external service down
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(
            ServiceUnavailableException ex, HttpServletRequest request) {
        log.error("Service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service Unavailable",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // ─── Circuit Breaker Exception ───────────────────────────────────────────

    /**
     * Handle Resilience4j circuit breaker open state.
     * When the circuit is OPEN, requests are immediately rejected to prevent
     * cascading failures to downstream services.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(
            CallNotPermittedException ex, HttpServletRequest request) {
        log.error("Circuit breaker is OPEN — request rejected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.withCircuitBreaker(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service Temporarily Unavailable",
                        "The service is currently experiencing issues and has been temporarily "
                                + "disabled to prevent further problems. Please try again in a few moments.",
                        request.getRequestURI(),
                        "OPEN"));
    }

    // ─── Spring Security Exceptions ──────────────────────────────────────────

    /**
     * Handle bad credentials during authentication
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials from {}", request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "Invalid username or password",
                        request.getRequestURI()));
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "Authentication failed: " + ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Handle access denied (403 Forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for {} on {}", request.getRemoteAddr(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        "You do not have permission to access this resource",
                        request.getRequestURI()));
    }

    // ─── Validation Exceptions ───────────────────────────────────────────────

    /**
     * Handle @Valid annotation validation failures.
     * Returns field-level validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Validation failed on {}: {}", request.getRequestURI(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.withValidation(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        "One or more fields have invalid values",
                        request.getRequestURI(),
                        errors));
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        String.format("Required parameter '%s' is missing", ex.getParameterName()),
                        request.getRequestURI()));
    }

    /**
     * Handle method argument type mismatch (e.g., String where Long expected)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        log.warn("Type mismatch: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        message,
                        request.getRequestURI()));
    }

    // ─── HTTP Exceptions ─────────────────────────────────────────────────────

    /**
     * Handle unsupported HTTP methods (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method {} not supported on {}", ex.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(
                        HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "Method Not Allowed",
                        String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
                        request.getRequestURI()));
    }

    /**
     * Handle unsupported media type (415)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Unsupported media type: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                        "Unsupported Media Type",
                        "Content type '" + ex.getContentType() + "' is not supported",
                        request.getRequestURI()));
    }

    /**
     * Handle malformed JSON request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        "Malformed JSON request body. Please check your request format.",
                        request.getRequestURI()));
    }

    /**
     * Handle 404 — no handler found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(
            NoHandlerFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL()),
                        request.getRequestURI()));
    }

    // ─── Database Exceptions ─────────────────────────────────────────────────

    /**
     * Handle database constraint violations (duplicate keys, FK violations, etc.)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "A database constraint was violated. ";
        String rootCauseMessage = ex.getMostSpecificCause().getMessage();

        if (rootCauseMessage != null) {
            if (rootCauseMessage.contains("Duplicate entry")) {
                message += "A record with the same unique value already exists.";
            } else if (rootCauseMessage.contains("foreign key constraint")) {
                message += "This record is referenced by other records and cannot be modified.";
            } else {
                message += "Please check your data and try again.";
            }
        }

        log.error("Data integrity violation: {}", rootCauseMessage);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "Data Conflict",
                        message,
                        request.getRequestURI()));
    }

    // ─── Illegal Argument ────────────────────────────────────────────────────

    /**
     * Handle illegal arguments
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // ─── RuntimeException (catch-all for business logic) ─────────────────────

    /**
     * Handle generic RuntimeExceptions thrown by services.
     * This acts as a safety net for un-migrated service code that still throws
     * raw RuntimeExceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Request Failed",
                        ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                        request.getRequestURI()));
    }

    // ─── Catch-All ───────────────────────────────────────────────────────────

    /**
     * Ultimate fallback for any unhandled exception.
     * Logs the full stack trace and returns a generic 500 response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {} - {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred. Please contact support if this persists.",
                        request.getRequestURI()));
    }
}
