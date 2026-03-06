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
@Table(name = "interviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication jobApplication;

    private String interviewRound; // e.g., "Round 1 - Technical", "Round 2 - HR"

    @Enumerated(EnumType.STRING)
    private InterviewType interviewType;

    @Column(columnDefinition = "DATETIME")
    private LocalDateTime scheduledDate;

    @ManyToOne
    @JoinColumn(name = "interviewer_id")
    private Employee interviewer;

    private String location; // Can be office address or virtual meeting location

    private String meetingLink; // For virtual interviews (Zoom, Teams, etc.)

    @Enumerated(EnumType.STRING)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    public enum InterviewType {
        PHONE_SCREENING, TECHNICAL, CODING_ROUND, HR_ROUND, MANAGER_ROUND, FINAL
    }

    public enum InterviewStatus {
        SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED, NO_SHOW
    }
}
