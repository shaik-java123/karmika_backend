package com.karmika.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeValidationResultDTO {

    private Long applicationId;

    private String candidateName;

    private String candidateEmail;

    private Boolean isValid;

    private Integer qualityScore; // 0-100

    private String validationStatus; // VALID, INVALID, NEEDS_REVIEW

    private String requiredSkillsMatch; // JSON - matched skills

    private String additionalSkillsFound; // JSON - extra skills found

    private String validationErrors; // JSON - list of errors/warnings

    private String resumeSummary; // Key info extracted from resume

    // Experience analysis
    private Integer extractedExperienceYears;

    private String mostRecentRole;

    private String mostRecentCompany;

    // Education analysis
    private String highestQualification;

    private String preferredField;

    // Recommendation
    private String recommendation; // SHORTLIST, MAYBE, REJECT

    private String recommendationReason;

    private LocalDateTime validatedAt;
}

