package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @Column(nullable = false)
    private String candidateName;

    @Column(nullable = false)
    private String candidateEmail;

    private String candidatePhone;

    @Column(length = 500)
    private String resumeUrl;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    private BigDecimal currentCtc;

    private BigDecimal expectedCtc;

    private Integer noticePeriodDays;

    private Integer totalExperienceYears;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    private LocalDate appliedDate;

    private String source; // e.g., LinkedIn, JobPortal, Internal, etc.

    // Resume Validation Fields
    private Boolean resumeValidated = false;

    private String resumeValidationErrors; // JSON string of validation errors

    @Column(columnDefinition = "TEXT")
    private String resumeSummary; // Extracted key info from resume

    private Integer resumeQualityScore; // 0-100 score

    private String requiredSkillsMatch; // JSON string of matched required skills

    private String additionalSkillsFound; // JSON string of additional skills found

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    public enum ApplicationStatus {
        APPLIED, SCREENING, INTERVIEW, OFFER, HIRED, REJECTED, WITHDRAWN, ON_HOLD
    }
}
