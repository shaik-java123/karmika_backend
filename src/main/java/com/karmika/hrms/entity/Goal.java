package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a performance goal/target assigned to an employee.
 * Goals can be linked to an appraisal cycle and are used to track
 * individual and team targets throughout the review period.
 */
@Entity
@Table(name = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The employee this goal is assigned to
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee assignedTo;

    // Who set the goal (manager / HR / admin)
    @ManyToOne
    @JoinColumn(name = "assigned_by_id")
    private Employee assignedBy;

    // Optional: link to an appraisal cycle
    @ManyToOne
    @JoinColumn(name = "cycle_id")
    private AppraisalCycle cycle;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Measurable target — what "done" looks like
    @Column(length = 500)
    private String targetMetric;

    // Numeric target value (e.g. 100 for "100 units")
    private Double targetValue;

    // Actual achieved value (updated by employee/manager)
    private Double achievedValue;

    // Progress 0–100
    @Column(nullable = false)
    private Integer progressPct = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalCategory category = GoalCategory.PERFORMANCE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalPriority priority = GoalPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.NOT_STARTED;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate completedDate;

    // Manager feedback / comments
    @Column(length = 1000)
    private String managerComments;

    // Employee self-assessment notes
    @Column(length = 1000)
    private String selfComments;

    // Weight as a % of overall performance (all goals should sum to ~100)
    @Column(nullable = false)
    private Integer weightage = 10;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    // ── Enums ──────────────────────────────────────────────────────────────

    public enum GoalStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        ON_HOLD
    }

    public enum GoalPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum GoalCategory {
        PERFORMANCE, // KPI-based business targets
        DEVELOPMENT, // Learning / upskilling
        BEHAVIORAL, // Soft skills / culture
        INNOVATION, // New ideas / process improvements
        CUSTOMER, // Customer satisfaction targets
        OPERATIONAL // Process / cost targets
    }
}
