package com.karmika.hrms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.User;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Ollama config (application.yml)
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:tinyllama:1.1b}")
    private String ollamaModel;

    // Ollama chat endpoint: POST /api/chat
    private String getChatUrl() {
        return ollamaBaseUrl + "/api/chat";
    }

    // ─── System Prompt ─────────────────────────────────────────────────────────
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

    // ─── POST /api/chatbot/chat ───────────────────────────────────────────────
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String userMessage = (String) request.get("message");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) request.getOrDefault("history",
                new ArrayList<>());

        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message cannot be empty"));
        }

        // ── User context ─────────────────────────────────────────────────────
        String userName = "there";
        String userRole = "EMPLOYEE";
        if (authentication != null) {
            try {
                User user = userRepository.findByUsername(authentication.getName()).orElse(null);
                if (user != null) {
                    Employee emp = employeeRepository.findByUser(user).orElse(null);
                    userName = emp != null ? emp.getFirstName() : user.getUsername();
                    userRole = user.getRole().name();
                }
            } catch (Exception ex) {
                System.err.println("[Chatbot] Could not load user context: " + ex.getMessage());
            }
        }

        try {
            String reply = callOllama(userMessage, history, userName, userRole);
            return ResponseEntity.ok(Map.of(
                    "reply", reply,
                    "timestamp", System.currentTimeMillis(),
                    "model", ollamaModel));

        } catch (ResourceAccessException ex) {
            // Ollama is not running
            System.err.println("[Chatbot] Ollama not reachable: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error",
                            "🔴 Ollama is not running. Please start it with: ollama serve",
                            "details", ex.getMessage()));

        } catch (HttpClientErrorException ex) {
            String body = ex.getResponseBodyAsString();
            System.err.println("[Chatbot] Ollama error " + ex.getStatusCode() + ": " + body);
            String msg = body;
            try {
                msg = objectMapper.readTree(body).path("error").asText(body);
            } catch (Exception ignored) {
            }
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", msg, "details", body));

        } catch (Exception ex) {
            System.err.println("[Chatbot] Unexpected error: " + ex.getClass().getSimpleName()
                    + " - " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                            "Something went wrong. Please try again.",
                            "details", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    // ─── Ollama /api/chat call ────────────────────────────────────────────────
    private String callOllama(
            String userMessage,
            List<Map<String, Object>> history,
            String userName,
            String userRole) throws Exception {

        RestTemplate restTemplate = new RestTemplate();
        ObjectNode requestBody = objectMapper.createObjectNode();

        requestBody.put("model", ollamaModel);
        requestBody.put("stream", false); // get full response at once

        // ── Build messages array ─────────────────────────────────────────────
        // Ollama uses OpenAI-compatible format: system / user / assistant roles
        ArrayNode messages = objectMapper.createArrayNode();

        // System message with context about the current user
        String systemWithContext = SYSTEM_PROMPT
                + " The current user's name is " + userName
                + " and their role is " + userRole + ".";

        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemWithContext);
        messages.add(systemMsg);

        // Conversation history (prior turns — does NOT include the current message)
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

        // Optional generation options to keep responses concise
        ObjectNode options = objectMapper.createObjectNode();
        options.put("temperature", 0.7);
        options.put("num_predict", 512); // max tokens
        options.put("top_p", 0.9);
        requestBody.set("options", options);

        // ── HTTP call ────────────────────────────────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String bodyJson = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

        System.out.println("[Chatbot] Calling Ollama at " + getChatUrl()
                + " with model=" + ollamaModel
                + ", turns=" + (messages.size()));

        ResponseEntity<String> response = restTemplate.postForEntity(
                getChatUrl(), entity, String.class);

        System.out.println("[Chatbot] Ollama HTTP status: " + response.getStatusCode());

        // ── Parse response ───────────────────────────────────────────────────
        // Ollama /api/chat response: { "message": { "role": "assistant", "content":
        // "..." }, "done": true }
        JsonNode root = objectMapper.readTree(response.getBody());

        String text = root.path("message").path("content").asText("").trim();

        if (text.isBlank()) {
            System.err.println("[Chatbot] Empty response from Ollama: " + response.getBody());
            return "I didn't get a response. Please try again.";
        }

        return text;
    }
}
