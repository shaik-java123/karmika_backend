package com.karmika.hrms.config;

import com.karmika.hrms.entity.User;
import com.karmika.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.karmika.hrms.repository.LeaveTypeRepository leaveTypeRepository;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            // Check if admin user exists
            if (!userRepository.existsByUsername("admin") && !userRepository.existsByEmail("admin@karmika.com")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@karmika.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(User.Role.ADMIN);
                admin.setActive(true);

                userRepository.save(admin);
                log.info("✅ Default admin user created:");
                log.info("   Username: admin");
                log.info("   Password: admin123");
                log.info("   Email: admin@karmika.com");
            } else {
                log.info("ℹ️  Admin user already exists");
            }

            // Create sample HR user if doesn't exist
            if (!userRepository.existsByUsername("hr") && !userRepository.existsByEmail("hr@karmika.com")) {
                User hr = new User();
                hr.setUsername("hr");
                hr.setEmail("hr@karmika.com");
                hr.setPassword(passwordEncoder.encode("hr123"));
                hr.setRole(User.Role.HR);
                hr.setActive(true);

                userRepository.save(hr);
                log.info("✅ Default HR user created:");
                log.info("   Username: hr");
                log.info("   Password: hr123");
            }

            // Create sample employee if doesn't exist
            if (!userRepository.existsByUsername("employee") && !userRepository.existsByEmail("employee@karmika.com")) {
                User employee = new User();
                employee.setUsername("employee");
                employee.setEmail("employee@karmika.com");
                employee.setPassword(passwordEncoder.encode("emp123"));
                employee.setRole(User.Role.EMPLOYEE);
                employee.setActive(true);

                userRepository.save(employee);
                log.info("✅ Default employee user created:");
                log.info("   Username: employee");
                log.info("   Password: emp123");
            }

            // Create sample Manager user if doesn't exist
            if (!userRepository.existsByUsername("manager") && !userRepository.existsByEmail("manager@karmika.com")) {
                User manager = new User();
                manager.setUsername("manager");
                manager.setEmail("manager@karmika.com");
                manager.setPassword(passwordEncoder.encode("manager123"));
                manager.setRole(User.Role.MANAGER);
                manager.setActive(true);

                userRepository.save(manager);
                log.info("✅ Default manager user created:");
                log.info("   Username: manager");
                log.info("   Password: manager123");
            }

            log.info("🚀 Database initialization completed!");

            // Initialize Leave Types
            if (leaveTypeRepository.count() == 0) {
                java.util.List<com.karmika.hrms.entity.LeaveType> defaultTypes = java.util.Arrays.asList(
                        new com.karmika.hrms.entity.LeaveType(null, "Casual Leave", "CASUAL_LEAVE",
                                "Paid leave for personal matters", true, 12),
                        new com.karmika.hrms.entity.LeaveType(null, "Sick Leave", "SICK_LEAVE",
                                "Paid leave for medical reasons", true, 10),
                        new com.karmika.hrms.entity.LeaveType(null, "Privilege Leave", "PRIVILEGE_LEAVE",
                                "Earned leave based on work days", true, 15),
                        new com.karmika.hrms.entity.LeaveType(null, "Maternity Leave", "MATERNITY_LEAVE",
                                "Paid leave for maternity", true, 180),
                        new com.karmika.hrms.entity.LeaveType(null, "Paternity Leave", "PATERNITY_LEAVE",
                                "Paid leave for paternity", true, 15),
                        new com.karmika.hrms.entity.LeaveType(null, "Work From Home", "WORK_FROM_HOME",
                                "Work from home request", true, 30),
                        new com.karmika.hrms.entity.LeaveType(null, "Comp Off", "COMP_OFF",
                                "Compensatory off for extra work", true, 0),
                        new com.karmika.hrms.entity.LeaveType(null, "Loss Of Pay", "LOSS_OF_PAY", "Unpaid leave", true,
                                0));
                leaveTypeRepository.saveAll(defaultTypes);
                log.info("✅ Default Leave Types initialized");
            }
        };
    }
}
