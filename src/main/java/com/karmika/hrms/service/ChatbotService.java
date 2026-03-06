package com.karmika.hrms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.karmika.hrms.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service for communicating with the Ollama AI chatbot.
 * Protected by a Resilience4j circuit breaker to prevent cascade failures
 * when Ollama is unavailable or responding slowly.
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:tinyllama:1.1b}")
    private String ollamaModel;

    private static final String SYSTEM_PROMPT = "You are Karma, a helpful HR Assistant for Karmika HRMS. " +
            "Karmika HRMS is a full-stack HR Management System for Indian companies. " +
            "It has these modules: " +
            "1. EMPLOYEE MANAGEMENT - employee profiles, departments, designations, reporting hierarchy. " +
            "2. LEAVE MANAGEMENT - apply leave (Casual, Sick, EL, Maternity), check balance, manager approval. Navigate to /leaves. "
            +
            "3. ATTENDANCE - daily check-in/check-out, working hours, late arrivals. Navigate to /attendance. " +
            "4. PAYROLL - monthly salary slips with earnings (Basic, HRA, Allowances) and deductions (PF, TDS). Navigate to /payroll. "
            +
            "5. APPRAISALS - 360 degree performance reviews, self/manager/peer review, 1-5 rating scale. Navigate to /appraisals. "
            +
            "6. GOALS - KPIs and OKRs with target values and progress tracking. Navigate to /appraisals. " +
            "7. ONBOARDING - new hire checklists, document uploads, HR review. Navigate to /onboarding. " +
            "8. TASKS - assigned work items with priority and due dates. Navigate to /tasks. " +
            "9. NOTIFICATIONS - in-app alerts for all events. " +
            "10. ORGANIZATION - departments, holidays, policies, announcements. Navigate to /organization. " +
            "11. DASHBOARD - role-based KPI overview. Navigate to /dashboard. " +
            "User roles: ADMIN (full access), HR (employee & leave management), FINANCE (payroll), MANAGER (team & tasks), EMPLOYEE (self-service). "
            +
            "Be friendly, concise, and helpful. Use bullet points for lists. " +
            "You do not have access to live database data - direct users to the relevant page. " +
            "Keep responses short and clear. Use simple English.";

    /**
     * Send a message to Ollama with circuit breaker protection.
     *
     * When the circuit is:
     * - CLOSED: calls go through normally
     * - OPEN: calls are immediately rejected (CallNotPermittedException)
     * - HALF_OPEN: a limited number of test calls are permitted
     *
     * Fallback returns a friendly message when Ollama is unreachable.
     */
    @CircuitBreaker(name = "ollamaService", fallbackMethod = "chatFallback")
    public Map<String, Object> chat(String userMessage, List<Map<String, Object>> history,
            String userName, String userRole) {
        try {
            String reply = callOllama(userMessage, history, userName, userRole);
            return Map.of(
                    "reply", reply,
                    "timestamp", System.currentTimeMillis(),
                    "model", ollamaModel);
        } catch (ResourceAccessException ex) {
            log.error("[Chatbot] Ollama not reachable: {}", ex.getMessage());
            throw new ServiceUnavailableException("Ollama",
                    "AI service is not running. Please start Ollama with: ollama serve", ex);
        } catch (HttpClientErrorException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("[Chatbot] Ollama error {}: {}", ex.getStatusCode(), body);
            String msg = body;
            try {
                msg = objectMapper.readTree(body).path("error").asText(body);
            } catch (Exception ignored) {
            }
            throw new ServiceUnavailableException("Ollama", msg, ex);
        } catch (Exception ex) {
            log.error("[Chatbot] Unexpected error: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw new ServiceUnavailableException("Ollama",
                    "An unexpected error occurred while communicating with the AI service", ex);
        }
    }

    /**
     * Fallback method invoked when the circuit breaker is OPEN.
     * Returns a friendly message instead of propagating the failure.
     */
    @SuppressWarnings("unused")
    private Map<String, Object> chatFallback(String userMessage, List<Map<String, Object>> history,
            String userName, String userRole, Throwable throwable) {
        log.warn("[Chatbot] Circuit breaker fallback triggered for user '{}': {}",
                userName, throwable.getMessage());

        String fallbackReply = "I'm sorry, but the AI assistant is temporarily unavailable. " +
                "The system has detected repeated connection issues and has paused requests " +
                "to avoid overloading. Please try again in about 30 seconds.\n\n" +
                "In the meantime, you can:\n" +
                "• Navigate to **/dashboard** for an overview\n" +
                "• Visit **/leaves** to manage your leaves\n" +
                "• Check **/attendance** for your attendance records\n" +
                "• Go to **/tasks** to view your tasks";

        return Map.of(
                "reply", fallbackReply,
                "timestamp", System.currentTimeMillis(),
                "model", "fallback",
                "circuitBreakerActive", true);
    }

    /**
     * Internal method to call Ollama /api/chat endpoint.
     */
    private String callOllama(String userMessage, List<Map<String, Object>> history,
            String userName, String userRole) throws Exception {

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", ollamaModel);
        requestBody.put("stream", false);

        // Build messages array
        ArrayNode messages = objectMapper.createArrayNode();

        // System message with user context
        String systemWithContext = SYSTEM_PROMPT
                + " The current user's name is " + userName
                + " and their role is " + userRole + ".";

        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemWithContext);
        messages.add(systemMsg);

        // Conversation history
        for (Map<String, Object> msg : history) {
            String role = "user".equals(msg.get("role")) ? "user" : "assistant";
            String content = (String) msg.getOrDefault("content", "");
            if (content.isBlank())
                continue;

            ObjectNode turn = objectMapper.createObjectNode();
            turn.put("role", role);
            turn.put("content", content);
            messages.add(turn);
        }

        // Current user message
        ObjectNode currentMsg = objectMapper.createObjectNode();
        currentMsg.put("role", "user");
        currentMsg.put("content", userMessage);
        messages.add(currentMsg);

        requestBody.set("messages", messages);

        // Generation options
        ObjectNode options = objectMapper.createObjectNode();
        options.put("temperature", 0.7);
        options.put("num_predict", 512);
        options.put("top_p", 0.9);
        requestBody.set("options", options);

        // HTTP call
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String bodyJson = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

        String chatUrl = ollamaBaseUrl + "/api/chat";
        log.info("[Chatbot] Calling Ollama at {} with model={}, turns={}",
                chatUrl, ollamaModel, messages.size());

        ResponseEntity<String> response = restTemplate.postForEntity(chatUrl, entity, String.class);
        log.info("[Chatbot] Ollama HTTP status: {}", response.getStatusCode());

        // Parse response
        JsonNode root = objectMapper.readTree(response.getBody());
        String text = root.path("message").path("content").asText("").trim();

        if (text.isBlank()) {
            log.warn("[Chatbot] Empty response from Ollama: {}", response.getBody());
            return "I didn't get a response. Please try again.";
        }

        return text;
    }

    public String getModel() {
        return ollamaModel;
    }
}
