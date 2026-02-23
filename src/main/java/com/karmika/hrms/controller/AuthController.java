package com.karmika.hrms.controller;

import com.karmika.hrms.dto.ApiResponse;
import com.karmika.hrms.dto.AuthResponse;
import com.karmika.hrms.dto.ChangePasswordRequest;
import com.karmika.hrms.dto.LoginRequest;
import com.karmika.hrms.dto.RegisterRequest;
import com.karmika.hrms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Invalid username or password");
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        try {
            authService.changePassword(authentication.getName(), request.getCurrentPassword(),
                    request.getNewPassword());
            return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Step 1: Request a password reset token (public endpoint)
     * Body: { "email": "user@example.com" }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Email is required"));
            }
            Map<String, Object> result = authService.forgotPassword(email.trim().toLowerCase());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Step 2: Reset password using the token (public endpoint)
     * Body: { "email": "...", "token": "123456", "newPassword": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            if (email == null || token == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Email, token and newPassword are required"));
            }
            authService.resetPassword(email.trim().toLowerCase(), token.trim(), newPassword);
            return ResponseEntity.ok(new ApiResponse(true, "Password reset successfully! You can now log in."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse> test() {
        return ResponseEntity.ok(new ApiResponse(true, "Karmika HRMS API is running! 🚀"));
    }
}
