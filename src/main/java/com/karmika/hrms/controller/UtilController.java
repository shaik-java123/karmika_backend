package com.karmika.hrms.controller;

import com.karmika.hrms.entity.User;
import com.karmika.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility controller for debugging and managing users (REMOVE IN PRODUCTION!)
 */
@RestController
@RequestMapping("/api/util")
@RequiredArgsConstructor
public class UtilController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUser(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", user.getId());
                    response.put("username", user.getUsername());
                    response.put("email", user.getEmail());
                    response.put("role", user.getRole());
                    response.put("active", user.getActive());
                    response.put("passwordLength", user.getPassword().length());
                    response.put("passwordPrefix",
                            user.getPassword().substring(0, Math.min(10, user.getPassword().length())));
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reset-password/{username}")
    public ResponseEntity<?> resetPassword(
            @PathVariable String username,
            @RequestParam String newPassword) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(user);

                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Password reset successfully");
                    response.put("username", username);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/test-password")
    public ResponseEntity<?> testPassword(
            @RequestParam String username,
            @RequestParam String password) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    boolean matches = passwordEncoder.matches(password, user.getPassword());

                    Map<String, Object> response = new HashMap<>();
                    response.put("username", username);
                    response.put("passwordMatches", matches);
                    response.put("passwordLength", user.getPassword().length());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new user (Admin utility endpoint)
     * POST /api/util/create-user
     * Body: { "username": "...", "password": "...", "email": "...", "role":
     * "ADMIN|HR|MANAGER|EMPLOYEE" }
     */
    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String email = request.get("email");
            String roleStr = request.get("role");

            // Validate required fields
            if (username == null || password == null || email == null || roleStr == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Missing required fields");
                error.put("required", new String[] { "username", "password", "email", "role" });
                return ResponseEntity.badRequest().body(error);
            }

            // Check if user already exists
            if (userRepository.existsByUsername(username)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Username already exists");
                error.put("username", username);
                return ResponseEntity.badRequest().body(error);
            }

            if (userRepository.existsByEmail(email)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Email already exists");
                error.put("email", email);
                return ResponseEntity.badRequest().body(error);
            }

            // Parse role
            User.Role role;
            try {
                role = User.Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid role");
                error.put("validRoles", new String[] { "ADMIN", "HR", "MANAGER", "EMPLOYEE" });
                return ResponseEntity.badRequest().body(error);
            }

            // Create new user
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setActive(true);

            User savedUser = userRepository.save(user);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("user", Map.of(
                    "id", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "email", savedUser.getEmail(),
                    "role", savedUser.getRole().name(),
                    "active", savedUser.getActive()));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create user");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Delete a user by username (Admin utility endpoint)
     */
    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    userRepository.delete(user);
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "User deleted successfully");
                    response.put("username", username);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "User not found");
                    error.put("username", username);
                    return ResponseEntity.notFound().build();
                });
    }
}
