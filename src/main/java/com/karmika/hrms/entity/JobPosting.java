package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_postings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobTitle;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    private Integer experienceMin;

    private Integer experienceMax;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String location;

    private Integer numberOfOpenings = 1;

    @ManyToOne
    @JoinColumn(name = "posted_by")
    private Employee postedBy;

    private LocalDate postedDate;

    private LocalDate closingDate;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    public enum EmploymentType {
        FULL_TIME, PART_TIME, CONTRACT, INTERN, TEMPORARY
    }

    public enum JobStatus {
        OPEN, CLOSED, ON_HOLD, FILLED
    }
}
