package com.karmika.hrms.service;

import com.karmika.hrms.dto.AuthResponse;
import com.karmika.hrms.dto.LoginRequest;
import com.karmika.hrms.dto.RegisterRequest;
import com.karmika.hrms.entity.User;
import com.karmika.hrms.repository.UserRepository;
import com.karmika.hrms.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setActive(true);

        userRepository.save(user);

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }

    public Map<String, Object> createUserForEmployee(String firstName, String lastName, String email, String roleName) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        String username = generateUsername(firstName, lastName);
        String password = generateRandomPassword();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        try {
            user.setRole(User.Role.valueOf(roleName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            user.setRole(User.Role.EMPLOYEE);
        }
        user.setActive(true);
        user.setPasswordChangeRequired(true);

        User savedUser = userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("user", savedUser);
        result.put("rawPassword", password);

        return result;
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);
    }

    private String generateUsername(String firstName, String lastName) {
        String baseUsername = (firstName.toLowerCase() + "." + lastName.toLowerCase()).replaceAll("[^a-z0-9.]", "");
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }
        return username;
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 10);
    }

    public AuthResponse login(LoginRequest request) {
        System.out.println("Login attempt for username: " + request.getUsername());

        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    System.out.println("User not found: " + request.getUsername());
                    return new RuntimeException("Invalid username or password");
                });

        System.out.println("User found: " + user.getUsername());
        System.out.println("User role: " + user.getRole());
        System.out.println("User active: " + user.getActive());
        System.out.println("Password from DB (first 20 chars): "
                + user.getPassword().substring(0, Math.min(20, user.getPassword().length())));
        System.out.println("Password from request: " + request.getPassword());

        // Manually check password using BCrypt
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        System.out.println("Password matches: " + passwordMatches);

        if (!passwordMatches) {
            System.out.println("Password mismatch!");
            throw new RuntimeException("Invalid username or password");
        }

        // Check if user is active
        if (!user.getActive()) {
            throw new RuntimeException("Account is disabled");
        }

        // Create UserDetails object for authentication
        org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getAuthorities());

        // Create authentication token with UserDetails (not just username string)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, // Changed from user.getUsername() to userDetails
                null,
                user.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        System.out.println("Login successful! Token generated.");
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(),
                user.getPasswordChangeRequired());
    }
}
