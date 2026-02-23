package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OnboardingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The employee this document belongs to */
    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Who uploaded this document */
    @ManyToOne
    @JoinColumn(name = "uploaded_by_id")
    private Employee uploadedBy;

    /**
     * HR_UPLOAD = document shared by HR with employee; EMPLOYEE_UPLOAD = employee's
     * own document
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentSource source = DocumentSource.HR_UPLOAD;

    @Column(nullable = false)
    private String documentName; // Display name

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    /** Server-side path or URL to the file */
    @Column(nullable = false, length = 500)
    private String filePath;

    /** Original filename from upload */
    private String originalFileName;

    /** MIME type of file */
    private String mimeType;

    /** File size in bytes */
    private Long fileSize;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum DocumentSource {
        HR_UPLOAD, // HR uploaded this for employee to read/sign
        EMPLOYEE_UPLOAD // Employee uploaded this themselves
    }

    public enum DocumentType {
        OFFER_LETTER,
        APPOINTMENT_LETTER,
        NDA,
        POLICY_DOCUMENT,
        HANDBOOK,
        ID_PROOF,
        ADDRESS_PROOF,
        EDUCATIONAL_CERTIFICATE,
        EXPERIENCE_LETTER,
        PAN_CARD,
        AADHAAR_CARD,
        PASSPORT,
        BANK_DETAILS,
        PHOTO,
        OTHER
    }
}
