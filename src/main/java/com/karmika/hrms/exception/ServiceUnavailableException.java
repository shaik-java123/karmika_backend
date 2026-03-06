package com.karmika.hrms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an external service (Ollama, email, etc.) is unavailable.
 * Maps to HTTP 503 Service Unavailable.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends RuntimeException {

    private String serviceName;

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String serviceName, String message) {
        super(String.format("Service '%s' is unavailable: %s", serviceName, message));
        this.serviceName = serviceName;
    }

    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(String.format("Service '%s' is unavailable: %s", serviceName, message), cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
