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
public class JobPostingDTO {

    private Long id;

    private String jobTitle;

    private Long departmentId;

    private String departmentName;

    private String jobDescription;

    private String requirements;

    private String employmentType;

    private Integer experienceMin;

    private Integer experienceMax;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String location;

    private Integer numberOfOpenings;

    private Long postedBy;

    private String postedByName;

    private LocalDate postedDate;

    private LocalDate closingDate;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long applicationsCount;
}

