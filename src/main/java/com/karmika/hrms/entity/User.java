package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.EMPLOYEE;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean passwordChangeRequired = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Method to get authorities for Spring Security
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // Add the base Role authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.name()));

        // Add feature-level authorities derived from the role
        authorities.addAll(this.role.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));

        return authorities;
    }

    public enum Role {
        ADMIN(Set.of(
                "FEATURE_MANAGE_USERS",
                "FEATURE_MANAGE_SETTINGS",
                "FEATURE_DELETE_RECORDS",
                "FEATURE_MANAGE_DEPARTMENTS",
                "FEATURE_VIEW_AUDIT_LOGS")),
        HR(Set.of(
                "FEATURE_MANAGE_EMPLOYEES",
                "FEATURE_MANAGE_LEAVES",
                "FEATURE_VIEW_PAYROLL",
                "FEATURE_ONBOARDING",
                "FEATURE_VIEW_REPORTS")),
        MANAGER(Set.of(
                "FEATURE_APPROVE_LEAVES",
                "FEATURE_VIEW_TEAM_REPORTS",
                "FEATURE_ASSIGN_TASKS",
                "FEATURE_VIEW_TEAM_ATTENDANCE")),
        EMPLOYEE(Set.of(
                "FEATURE_VIEW_SELF_PROFILE",
                "FEATURE_APPLY_LEAVE",
                "FEATURE_VIEW_SELF_TASKS"));

        private final Set<String> permissions;

        Role(Set<String> permissions) {
            this.permissions = permissions;
        }

        public Set<String> getPermissions() {
            return permissions;
        }
    }
}
