package com.karmika.hrms.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DemoApiService {

    private static final Logger log = LoggerFactory.getLogger(DemoApiService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @CircuitBreaker(name = "randomUserService", fallbackMethod = "fallbackGetUsers")
    public String getRandomUsers(int count) {
        log.info("Fetching {} random users from external API...", count);
        String url = "https://randomuser.me/api/?results=" + count;
        return restTemplate.getForObject(url, String.class);
    }

    public String fallbackGetUsers(int count, Throwable t) {
        log.error("Fallback triggered for randomUserService. Reason: {}", t.getMessage());
        return "{\"results\": [], \"fallback\": true, \"message\": \"Service is temporarily unavailable or circuit is open.\"}";
    }
}
