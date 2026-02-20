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
@Table(name = "competencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Competency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // e.g., COMM, LEAD, TECH

    @Column(nullable = false)
    private String name; // e.g., Communication, Leadership

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompetencyCategory category;

    @Column(nullable = false)
    private Integer weightage = 10; // Percentage weight in overall rating

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    public enum CompetencyCategory {
        TECHNICAL, // Technical skills
        BEHAVIORAL, // Soft skills, behavior
        LEADERSHIP, // Leadership qualities
        CORE_VALUES, // Company values
        FUNCTIONAL, // Job-specific skills
        MANAGERIAL // Management skills
    }
}
