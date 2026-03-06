package com.karmika.hrms.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for monitoring circuit breaker states with live health
 * probes.
 * Shows both the circuit breaker state (traffic policy) and actual service
 * connectivity.
 * Accessible only to ADMIN users.
 */
@RestController
@RequestMapping("/api/admin/circuit-breakers")
@RequiredArgsConstructor
public class CircuitBreakerMonitorController {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerMonitorController.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RestTemplate healthCheckRestTemplate = new RestTemplate();

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    /**
     * Get the status of all circuit breakers including live health probes.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllCircuitBreakerStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("timestamp", System.currentTimeMillis());

        var breakers = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .map(cb -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", cb.getName());
                    info.put("state", cb.getState().name());
                    info.put("failureRate", cb.getMetrics().getFailureRate());
                    info.put("failureRateThreshold",
                            cb.getCircuitBreakerConfig().getFailureRateThreshold());
                    info.put("slowCallRate", cb.getMetrics().getSlowCallRate());
                    info.put("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
                    info.put("failedCalls", cb.getMetrics().getNumberOfFailedCalls());
                    info.put("successfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
                    info.put("notPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls());

                    // State explanation
                    String stateDescription = switch (cb.getState()) {
                        case CLOSED -> "Normal operation — all calls passing through";
                        case OPEN -> "Circuit is OPEN — calls are being rejected to prevent cascade failure";
                        case HALF_OPEN -> "Testing recovery — limited calls allowed to check if service is back";
                        case DISABLED -> "Circuit breaker is disabled";
                        case FORCED_OPEN -> "Manually forced open by administrator";
                        case METRICS_ONLY -> "Monitoring only — not blocking any calls";
                    };
                    info.put("stateDescription", stateDescription);

                    // ── Live Health Probe ────────────────────────────────────
                    Map<String, Object> healthProbe = probeServiceHealth(cb.getName());
                    info.put("serviceReachable", healthProbe.get("reachable"));
                    info.put("serviceLatencyMs", healthProbe.get("latencyMs"));
                    info.put("serviceHealthMessage", healthProbe.get("message"));

                    return info;
                })
                .collect(Collectors.toList());

        response.put("circuitBreakers", breakers);
        response.put("totalBreakers", breakers.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Perform a quick connectivity check for the service behind a circuit breaker.
     * This does NOT go through the circuit breaker — it's a separate, lightweight
     * probe.
     */
    private Map<String, Object> probeServiceHealth(String breakerName) {
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();

        try {
            switch (breakerName) {
                case "ollamaService" -> {
                    // Ping Ollama's root endpoint (returns version info if running)
                    String probeUrl = ollamaBaseUrl + "/api/tags";
                    ResponseEntity<String> resp = healthCheckRestTemplate.getForEntity(probeUrl, String.class);
                    long latency = System.currentTimeMillis() - start;
                    result.put("reachable", resp.getStatusCode().is2xxSuccessful());
                    result.put("latencyMs", latency);
                    result.put("message", "Ollama is running (responded in " + latency + "ms)");
                }
                case "randomUserService" -> {
                    // Ping RandomUser API directly as health probe measure
                    String probeUrl = "https://randomuser.me/api/";
                    ResponseEntity<String> resp = healthCheckRestTemplate.getForEntity(probeUrl, String.class);
                    long latency = System.currentTimeMillis() - start;
                    result.put("reachable", resp.getStatusCode().is2xxSuccessful());
                    result.put("latencyMs", latency);
                    result.put("message", "RandomUser.me is online (responded in " + latency + "ms)");
                }
                default -> {
                    // For services without a known health endpoint, mark as unknown
                    result.put("reachable", null);
                    result.put("latencyMs", null);
                    result.put("message", "No health probe configured for this service");
                }
            }
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            log.warn("[HealthProbe] {} is unreachable: {}", breakerName, ex.getMessage());
            result.put("reachable", false);
            result.put("latencyMs", latency);
            result.put("message", "Service is not reachable — " + getRootCauseMessage(ex));
        }

        return result;
    }

    private String getRootCauseMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        if (msg != null && msg.contains("Connection refused")) {
            return "Connection refused (service is not running)";
        }
        return msg != null ? msg : cause.getClass().getSimpleName();
    }
}
