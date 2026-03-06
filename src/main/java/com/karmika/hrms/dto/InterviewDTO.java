package com.karmika.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDTO {

    private Long id;

    private Long jobApplicationId;

    private String candidateName;

    private String candidateEmail;

    private String interviewRound;

    private String interviewType;

    private LocalDateTime scheduledDate;

    private Long interviewerId;

    private String interviewerName;

    private String location;

    private String meetingLink;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Feedback summary
    private Long feedbackCount;

    private Double averageRating;
}

