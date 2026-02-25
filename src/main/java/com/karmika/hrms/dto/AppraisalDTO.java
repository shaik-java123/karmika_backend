package com.karmika.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppraisalDTO {
    private Long id;
    private Long cycleId;
    private String cycleName;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String designation;
    private String department;
    private String status;

    // Review Status
    private Boolean selfReviewCompleted;
    private String selfReviewCompletedAt;
    private Boolean managerReviewCompleted;
    private String managerReviewCompletedAt;
    private Integer peerReviewsCompleted;
    private Integer peerReviewsRequired;
    private Integer subordinateReviewsCompleted;
    private Integer subordinateReviewsRequired;

    // Manager Info
    private Long managerId;
    private String managerName;

    // Ratings
    private Double selfRating;
    private Double managerRating;
    private Double peerAverageRating;
    private Double subordinateAverageRating;
    private Double overallRating;

    // Comments
    private String managerComments;
    private String employeeComments;
    private String strengths;
    private String areasOfImprovement;
    private String goals;
    private String performanceRating;

    // Agreement Tracking
    private Boolean employeeAgreed;
    private String employeeDisagreeComments;

    // Approval
    private String approvedBy;
    private String approvedAt;
    private String approvalRemarks;

    // Reviews
    private List<AppraisalReviewDTO> reviews;

    // Competency Ratings Summary
    private Map<String, Double> competencyRatings;
}
