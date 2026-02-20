package com.karmika.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppraisalCycleDTO {
    private Long id;
    private String cycleName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate reviewPeriodStart;
    private LocalDate reviewPeriodEnd;
    private String status;
    private String cycleType;
    private Boolean enableSelfReview;
    private Boolean enableManagerReview;
    private Boolean enablePeerReview;
    private Boolean enableSubordinateReview;
    private Integer minPeerReviewers;
    private Integer maxPeerReviewers;
    private String createdBy;
    private String createdAt;

    // Statistics
    private Integer totalAppraisals;
    private Integer completedAppraisals;
    private Integer pendingAppraisals;
}
