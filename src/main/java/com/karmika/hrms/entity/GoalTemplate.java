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
import java.util.ArrayList;
import java.util.List;

/**
 * A goal-set template that a manager (or HR) creates once per cycle.
 * When the template is PUBLISHED the system fans it out, creating a
 * {@link Goal} record for every direct report of the creator.
 *
 * Workflow:
 * DRAFT → manager adds/removes GoalMetric rows
 * PUBLISHED → direct-reports can see their individual Goal copies and
 * start filling actual values
 * LOCKED → employee submission window closed; manager can rate
 */
@Entity
@Table(name = "goal_templates", uniqueConstraints = @UniqueConstraint(name = "uq_template_manager_cycle", columnNames = {
        "manager_id", "cycle_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GoalTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "manager_id", nullable = false)
    private Employee manager;

    @ManyToOne
    @JoinColumn(name = "cycle_id", nullable = false)
    private AppraisalCycle cycle;

    @Column(nullable = false, length = 200)
    private String templateName;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status = TemplateStatus.DRAFT;

    // Deadline for employees to fill in their actual values
    @Column(nullable = false)
    private LocalDate employeeSubmissionDeadline;

    // When the manager published (fans out Goals to employees)
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime publishedAt;

    // When the manager locked for rating
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime lockedAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalMetric> metrics = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedAt;

    // ── Enums ─────────────────────────────────────────────────────────────

    public enum TemplateStatus {
        DRAFT, // Manager is still editing
        PUBLISHED, // Visible to employees; they can fill actuals
        LOCKED // Submission closed; manager rates
    }
}
