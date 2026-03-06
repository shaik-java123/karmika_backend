package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class InterviewFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @ManyToOne
    @JoinColumn(name = "interviewer_id", nullable = false)
    private Employee interviewer;

    private BigDecimal rating; // Overall rating (e.g., out of 5)

    private BigDecimal technicalSkillsRating;

    private BigDecimal communicationRating;

    private BigDecimal culturalFitRating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Enumerated(EnumType.STRING)
    private Recommendation recommendation = Recommendation.PENDING;

    @Column(columnDefinition = "DATETIME")
    private LocalDateTime submittedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    public enum Recommendation {
        STRONG_YES, YES, MAYBE, NO, STRONG_NO, PENDING
    }
}
