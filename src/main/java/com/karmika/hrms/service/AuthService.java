package com.karmika.hrms.service;

import com.karmika.hrms.dto.AuthResponse;
import com.karmika.hrms.dto.LoginRequest;
import com.karmika.hrms.dto.RegisterRequest;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.User;
import com.karmika.hrms.exception.BadRequestException;
import com.karmika.hrms.exception.DuplicateResourceException;
import com.karmika.hrms.exception.ResourceNotFoundException;
import com.karmika.hrms.exception.UnauthorizedException;
import com.karmika.hrms.repository.EmployeeRepository;
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
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
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
            throw new DuplicateResourceException("User", "email", email);
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
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UnauthorizedException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);
    }

    /**
     * Generates a 6-digit reset token for the given email.
     * Token expires in 15 minutes.
     * Since no SMTP is configured, the token is returned in the response
     * so an admin can share it with the user securely.
     */
    public Map<String, Object> forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!user.getActive()) {
            throw new BadRequestException("This account is disabled. Contact your administrator.");
        }

        // Generate a 6-digit numeric OTP
        SecureRandom random = new SecureRandom();
        String token = String.format("%06d", random.nextInt(1_000_000));

        user.setResetToken(token);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Reset token generated. Share this token with the user to reset their password.");
        result.put("username", user.getUsername());
        result.put("token", token); // Displayed in UI since no email is configured
        result.put("expiresInMinutes", 15);
        return result;
    }

    /**
     * Resets password using the token issued by forgotPassword.
     */
    public void resetPassword(String email, String token, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getResetToken() == null || !user.getResetToken().equals(token)) {
            throw new BadRequestException("Invalid reset token. Please request a new one.");
        }

        if (user.getResetTokenExpiry() == null ||
                java.time.LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            // Clear expired token
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new BadRequestException("This reset token has expired. Please request a new one.");
        }

        if (newPassword.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
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
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        // Validate password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatches) {
            throw new UnauthorizedException("Invalid username or password");
        }

        // Check if user is active
        if (!user.getActive()) {
            throw new BadRequestException("Account is disabled. Contact your administrator.");
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

        // Look up the associated Employee record to include employeeId in the response
        Long employeeId = employeeRepository.findByEmail(user.getEmail())
                .map(Employee::getId)
                .orElse(null);

        return new AuthResponse(token, user.getId(), employeeId, user.getUsername(),
                user.getEmail(), user.getRole().name(), user.getPasswordChangeRequired());
    }
}
