package com.karmika.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppraisalReviewDTO {
    private Long id;
    private Long appraisalId;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerType;
    private String status;
    private String overallComments;
    private String strengths;
    private String areasOfImprovement;
    private Double overallRating;
    private String submittedAt;
    private Boolean isAnonymous;

    // Competency Ratings
    private List<CompetencyRatingDTO> ratings;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CompetencyRatingDTO {
    private Long id;
    private Long competencyId;
    private String competencyName;
    private String competencyCode;
    private Integer rating;
    private String comments;
}
