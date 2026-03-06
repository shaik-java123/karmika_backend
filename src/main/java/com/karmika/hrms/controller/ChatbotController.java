package com.karmika.hrms.controller;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.User;
import com.karmika.hrms.exception.BadRequestException;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.UserRepository;
import com.karmika.hrms.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST controller for the AI chatbot.
 * Delegates to ChatbotService which is protected by a circuit breaker.
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String userMessage = (String) request.get("message");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) request.getOrDefault("history",
                new ArrayList<>());

        if (userMessage == null || userMessage.isBlank()) {
            throw new BadRequestException("Message cannot be empty");
        }

        // Resolve user context
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
                log.warn("[Chatbot] Could not load user context: {}", ex.getMessage());
            }
        }

        // Delegate to circuit-breaker-protected service
        Map<String, Object> result = chatbotService.chat(userMessage, history, userName, userRole);
        return ResponseEntity.ok(result);
    }
}
