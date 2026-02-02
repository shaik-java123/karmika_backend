package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "salary_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SalaryComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // e.g., BASIC, HRA, DA, PF, TAX

    @Column(nullable = false)
    private String name; // e.g., Basic Salary, House Rent Allowance

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentType type; // EARNING or DEDUCTION

    @Enumerated(EnumType.STRING)
    private CalculationType calculationType; // FIXED, PERCENTAGE_OF_BASIC, PERCENTAGE_OF_GROSS

    private Double defaultPercentage; // For percentage-based components

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isMandatory = false; // e.g., PF, ESI are mandatory

    @Column(length = 500)
    private String description;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum ComponentType {
        EARNING,
        DEDUCTION
    }

    public enum CalculationType {
        FIXED,
        PERCENTAGE_OF_BASIC,
        PERCENTAGE_OF_GROSS
    }
}
