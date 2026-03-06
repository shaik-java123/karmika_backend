package com.karmika.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationDTO {

    private Long id;

    private Long jobPostingId;

    private String jobTitle;

    private String candidateName;

    private String candidateEmail;

    private String candidatePhone;

    private String resumeUrl;

    private String coverLetter;

    private BigDecimal currentCtc;

    private BigDecimal expectedCtc;

    private Integer noticePeriodDays;

    private Integer totalExperienceYears;

    private String status;

    private LocalDate appliedDate;

    private String source;

    // Resume Validation Fields
    private Boolean resumeValidated;

    private String resumeValidationErrors;

    private String resumeSummary;

    private Integer resumeQualityScore;

    private String requiredSkillsMatch;

    private String additionalSkillsFound;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // For interview tracking
    private Integer interviewCount;

    private String latestInterviewStatus;
}

