package com.karmika.hrms.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for testing and data
 * initialization
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("=== Password Hash Generator ===");
        System.out.println();
        System.out.println("admin123 => " + encoder.encode("admin123"));
        System.out.println("hr123 => " + encoder.encode("hr123"));
        System.out.println("emp123 => " + encoder.encode("emp123"));
        System.out.println();

        // Test validation
        String testPassword = "admin123";
        String testHash = encoder.encode(testPassword);
        System.out.println("Test: Does 'admin123' match its hash? " + encoder.matches(testPassword, testHash));
    }
}
