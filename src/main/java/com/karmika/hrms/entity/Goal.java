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
 * An individual employee's goal record.
 *
 * Two creation paths:
 * 1. Via GoalTemplate.publish() → templateRef + metricRef are set;
 * one row per direct-report × metric.
 * 2. Legacy / standalone form → created directly (templateRef = null).
 *
 * Visibility lifecycle:
 * isVisibleToEmployee = false (draft / before publish)
 * isVisibleToEmployee = true (after manager publishes template)
 * employeeSubmitted = true (employee saved their actuals)
 * managerApproved = true (manager approved → feeds into rating)
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

    // ── Assignment ─────────────────────────────────────────────────────

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee assignedTo;

    @ManyToOne
    @JoinColumn(name = "assigned_by_id")
    private Employee assignedBy;

    @ManyToOne
    @JoinColumn(name = "cycle_id")
    private AppraisalCycle cycle;

    // ── Template linkage (optional) ────────────────────────────────────

    /** The GoalTemplate this row was generated from (null for standalone goals) */
    @ManyToOne
    @JoinColumn(name = "template_id")
    private GoalTemplate template;

    /** The specific metric row inside the template (null for standalone goals) */
    @ManyToOne
    @JoinColumn(name = "metric_id")
    private GoalMetric metric;

    // ── Content ────────────────────────────────────────────────────────

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Pillar / category (denormalised from GoalMetric for fast filtering)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalMetric.MetricPillar pillar = GoalMetric.MetricPillar.DELIVERY_EXECUTION;

    @Column(length = 100)
    private String unit;

    // ── Targets ────────────────────────────────────────────────────────

    @Column(length = 500)
    private String targetMetric; // human-readable label (legacy compat)

    private Double targetValue; // Manager sets this

    private Double achievedValue; // Employee fills this

    @Column(nullable = false)
    private Integer progressPct = 0;

    // ── Weight & Priority ──────────────────────────────────────────────

    @Column(nullable = false)
    private Integer weightage = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalPriority priority = GoalPriority.MEDIUM;

    // ── Status ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.NOT_STARTED;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate completedDate;

    // ── Visibility / approval flags ────────────────────────────────────

    /**
     * False until the manager publishes the template.
     * When false the employee cannot see this goal at all.
     */
    @Column(nullable = false)
    private Boolean isVisibleToEmployee = false;

    /**
     * True once the employee has saved their actual values
     * and self-assessment and clicked "Submit".
     */
    @Column(nullable = false)
    private Boolean employeeSubmitted = false;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime employeeSubmittedAt;

    /**
     * True once the manager has reviewed the employee's submission.
     * Only goals with managerApproved = true contribute to the rating score.
     */
    @Column(nullable = false)
    private Boolean managerApproved = false;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime managerApprovedAt;

    // ── Comments ───────────────────────────────────────────────────────

    @Column(length = 1000)
    private String managerComments;

    @Column(length = 1000)
    private String selfComments;

    // ── Audit ──────────────────────────────────────────────────────────

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    // ── Enums ──────────────────────────────────────────────────────────

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

    /**
     * Legacy category enum kept for standalone (non-template) goals.
     */
    public enum GoalCategory {
        PERFORMANCE, DEVELOPMENT, BEHAVIORAL, INNOVATION, CUSTOMER, OPERATIONAL
    }
}
