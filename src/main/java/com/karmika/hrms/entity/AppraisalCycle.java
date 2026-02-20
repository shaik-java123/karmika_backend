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

@Entity
@Table(name = "appraisal_cycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AppraisalCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String cycleName; // e.g., "Q1 2026 Performance Review"

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate reviewPeriodStart; // Period being reviewed

    @Column(nullable = false)
    private LocalDate reviewPeriodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CycleStatus status = CycleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CycleType cycleType = CycleType.ANNUAL;

    @Column(nullable = false)
    private Boolean enableSelfReview = true;

    @Column(nullable = false)
    private Boolean enableManagerReview = true;

    @Column(nullable = false)
    private Boolean enablePeerReview = true;

    @Column(nullable = false)
    private Boolean enableSubordinateReview = false;

    @Column(nullable = false)
    private Integer minPeerReviewers = 2;

    @Column(nullable = false)
    private Integer maxPeerReviewers = 5;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Employee createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    public enum CycleStatus {
        DRAFT, // Being configured
        ACTIVE, // Currently running
        COMPLETED, // Finished
        CANCELLED // Cancelled
    }

    public enum CycleType {
        ANNUAL, // Yearly review
        SEMI_ANNUAL, // Half-yearly
        QUARTERLY, // Every 3 months
        PROBATION, // Probation review
        PROJECT_END // End of project
    }
}
