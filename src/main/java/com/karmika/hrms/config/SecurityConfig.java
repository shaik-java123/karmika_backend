package com.karmika.hrms.config;

import com.karmika.hrms.security.CustomUserDetailsService;
import com.karmika.hrms.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/util/**").permitAll() // Debug utilities - Remove in production!
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/uploads/**").permitAll() // Serve uploaded files (onboarding docs, photos)
                                                                    // publicly

                        // Actuator health & circuit breaker monitoring
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/actuator/circuitbreakers/**").permitAll()
                        .requestMatchers("/actuator/circuitbreakerevents/**").permitAll()

                        // Swagger UI / OpenAPI
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()
                        // ADMIN-only endpoints - Full system control
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/create").hasRole("ADMIN")
                        .requestMatchers("/api/users/delete/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/role/**").hasRole("ADMIN")
                        .requestMatchers("/api/departments/create").hasRole("ADMIN")
                        .requestMatchers("/api/departments/delete/**").hasRole("ADMIN")

                        // HR endpoints - Employee & Leave management
                        .requestMatchers("/api/hr/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/employees/create").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/employees/update/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/employees/list").hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/employees/view/**").hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/leave/all").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/leave/balance/**").hasAnyRole("ADMIN", "HR")

                        // MANAGER endpoints - Team management & approvals
                        .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers("/api/leave/approve/**").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers("/api/leave/reject/**").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers("/api/leave/team").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers("/api/attendance/team").hasAnyRole("ADMIN", "HR", "MANAGER")

                        // EMPLOYEE endpoints - Self-service
                        .requestMatchers("/api/employee/**").authenticated()
                        .requestMatchers("/api/leave/apply").authenticated()
                        .requestMatchers("/api/leave/my-leaves").authenticated()
                        .requestMatchers("/api/leave/cancel/**").authenticated()
                        .requestMatchers("/api/attendance/checkin").authenticated()
                        .requestMatchers("/api/attendance/checkout").authenticated()
                        .requestMatchers("/api/attendance/my-attendance").authenticated()
                        .requestMatchers("/api/profile/**").authenticated()

                        // COMMON - All authenticated users
                        .requestMatchers("/api/departments/list").authenticated()
                        .requestMatchers("/api/dashboard/**").authenticated()

                        // APPRAISAL endpoints
                        .requestMatchers("/api/appraisals/competencies").authenticated()
                        .requestMatchers("/api/appraisals/my-appraisals").authenticated()
                        .requestMatchers("/api/appraisals/my-reviews/**").authenticated()
                        .requestMatchers("/api/appraisals/reviews/**").authenticated()
                        .requestMatchers("/api/appraisals/*/peer-reviewers").authenticated()
                        .requestMatchers("/api/appraisals/cycles/active").authenticated()
                        .requestMatchers("/api/appraisals/cycles").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers("/api/appraisals/cycles/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/appraisals/*/approve").hasAnyRole("ADMIN", "HR")

                        // PAYROLL endpoints
                        .requestMatchers("/api/payroll/my-slips").authenticated()
                        .requestMatchers("/api/payroll/slip/{id}").authenticated()
                        .requestMatchers("/api/payroll/**").hasAnyRole("ADMIN", "HR", "FINANCE")

                        // LMS endpoints
                        .requestMatchers("/api/lms/certificate/**").permitAll()
                        .requestMatchers("/api/lms/courses/*/publish").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/lms/courses/*/archive").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/lms/courses/*/delete").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/api/lms/**").authenticated()

                        // Default - require authentication
                        .requestMatchers("/api/chatbot/**").authenticated()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
