package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    public enum NotificationType {
        LEAVE_APPROVED,
        LEAVE_REJECTED,
        LEAVE_APPLIED,
        LEAVE_CANCELLED,
        ATTENDANCE_REMINDER,
        SYSTEM_ALERT,
        TASK_ASSIGNED,
        TASK_COMPLETED,
        ANNOUNCEMENT,
        INFO,
        ONBOARDING_STARTED,
        ONBOARDING_TASK_SUBMITTED,
        ONBOARDING_TASK_APPROVED,
        ONBOARDING_TASK_REJECTED,
        ONBOARDING_TASK_WAIVED,
        ONBOARDING_DOCUMENT_SHARED
    }
}
