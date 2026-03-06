package com.karmika.hrms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A scheduled virtual class / live session for a course,
 * backed by a Google Meet meeting link.
 */
@Entity
@Table(name = "lms_virtual_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class VirtualClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String agenda;

    /** Google Meet / Zoom / Teams external link */
    @Column(length = 1000)
    private String meetingLink;

    /** e.g. "google_meet", "zoom", "teams", "other" */
    private String platform = "google_meet";

    /** Google Calendar / Meet event ID for future management */
    @Column(length = 500)
    private String externalEventId;

    private LocalDateTime scheduledAt;

    /** Duration in minutes */
    private Integer durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.SCHEDULED;

    /** Recording URL after the session ends */
    @Column(length = 1000)
    private String recordingUrl;

    @ManyToOne
    @JoinColumn(name = "host_employee_id")
    private Employee host;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    public enum SessionStatus {
        SCHEDULED, LIVE, COMPLETED, CANCELLED
    }
}
