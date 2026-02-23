package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_checklists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OnboardingChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The employee this checklist item belongs to */
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** HR user who created this checklist item */
    @ManyToOne
    @JoinColumn(name = "assigned_by_id")
    private Employee assignedBy;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType = TaskType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChecklistStatus status = ChecklistStatus.PENDING;

    private LocalDate dueDate;

    /** Notes from HR (instructions, links, etc.) */
    @Column(length = 1000)
    private String hrNotes;

    /** Notes/comments from employee when submitting */
    @Column(length = 1000)
    private String employeeNotes;

    /** Path to the file uploaded by employee as proof/completion */
    @Column(length = 500)
    private String attachmentPath;

    /** Original filename of the uploaded file */
    private String attachmentName;

    private LocalDateTime completedAt;

    /** HR can reject with a reason and send back to PENDING */
    @Column(length = 500)
    private String rejectionReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum TaskType {
        UPLOAD_PHOTO, // Employee must upload a clear profile photo
        UPLOAD_DOCUMENT, // Employee must upload a specific document
        FILL_FORM, // Employee must fill a form
        SIGN_DOCUMENT, // Employee must sign and upload
        GENERAL, // General action item
        ACKNOWLEDGEMENT // Employee must acknowledge a policy
    }

    public enum ChecklistStatus {
        PENDING, // Not yet started by employee
        IN_REVIEW, // Employee submitted, waiting for HR review
        APPROVED, // HR approved the submission
        REJECTED, // HR rejected, sent back to employee
        WAIVED // HR waived this requirement
    }
}
