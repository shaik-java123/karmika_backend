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
@Table(name = "appraisals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Appraisal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cycle_id", nullable = false)
    private AppraisalCycle cycle;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee; // Employee being reviewed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppraisalStatus status = AppraisalStatus.NOT_STARTED;

    // Self Review
    @Column(nullable = false)
    private Boolean selfReviewCompleted = false;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime selfReviewCompletedAt;

    // Manager Review
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(nullable = false)
    private Boolean managerReviewCompleted = false;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime managerReviewCompletedAt;

    // Peer Reviews
    @Column(nullable = false)
    private Integer peerReviewsCompleted = 0;

    @Column(nullable = false)
    private Integer peerReviewsRequired = 0;

    // Subordinate Reviews
    @Column(nullable = false)
    private Integer subordinateReviewsCompleted = 0;

    @Column(nullable = false)
    private Integer subordinateReviewsRequired = 0;

    // Overall Ratings
    private Double selfRating;
    private Double managerRating;
    private Double peerAverageRating;
    private Double subordinateAverageRating;
    private Double overallRating;

    // Final Review
    @Column(length = 2000)
    private String managerComments;

    @Column(length = 2000)
    private String employeeComments;

    @Column(length = 1000)
    private String strengths;

    @Column(length = 1000)
    private String areasOfImprovement;

    @Column(length = 1000)
    private String goals; // Goals for next period

    @Enumerated(EnumType.STRING)
    private PerformanceRating performanceRating;

    // Agreement Tracking
    @Column(nullable = true)
    private Boolean employeeAgreed;

    @Column(length = 2000)
    private String employeeDisagreeComments;

    // Approval
    @ManyToOne
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String approvalRemarks;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    public enum AppraisalStatus {
        NOT_STARTED, // Not yet started
        IN_PROGRESS, // Reviews in progress
        PENDING_MANAGER, // Waiting for manager review
        COMPLETED, // All reviews completed
        APPROVED, // Approved by HR/Admin
        CANCELLED // Cancelled
    }

    public enum PerformanceRating {
        OUTSTANDING, // 5 - Exceptional performance
        EXCEEDS, // 4 - Exceeds expectations
        MEETS, // 3 - Meets expectations
        NEEDS_IMPROVEMENT, // 2 - Needs improvement
        UNSATISFACTORY // 1 - Unsatisfactory
    }
}
