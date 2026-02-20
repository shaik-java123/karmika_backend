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
@Table(name = "appraisal_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AppraisalReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "appraisal_id", nullable = false)
    private Appraisal appraisal;

    @ManyToOne
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Employee reviewer; // Person giving the review

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewerType reviewerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(length = 2000)
    private String overallComments;

    @Column(length = 1000)
    private String strengths;

    @Column(length = 1000)
    private String areasOfImprovement;

    private Double overallRating; // Calculated from competency ratings

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private Boolean isAnonymous = false; // For peer reviews

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    public enum ReviewerType {
        SELF, // Self review
        MANAGER, // Direct manager
        PEER, // Colleague/Peer
        SUBORDINATE, // Team member reporting to employee
        SKIP_LEVEL // Manager's manager
    }

    public enum ReviewStatus {
        PENDING, // Not yet started
        IN_PROGRESS, // Started but not submitted
        SUBMITTED, // Completed and submitted
        EXPIRED // Deadline passed
    }
}
