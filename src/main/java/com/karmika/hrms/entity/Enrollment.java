package com.karmika.hrms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "lms_enrollments", uniqueConstraints = @UniqueConstraint(columnNames = { "course_id", "employee_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    /**
     * Expose just the course ID in JSON (the full Course object is @JsonIgnore'd)
     */
    @JsonProperty("courseId")
    public Long extractCourseId() {
        return course != null ? course.getId() : null;
    }

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    /** Overall completion percentage 0-100 */
    private Integer progressPercent = 0;

    private LocalDateTime completedAt;

    /** Certificate download URL once course is completed */
    @Column(length = 500)
    private String certificateUrl;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime enrolledAt;

    @LastModifiedDate
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    public enum EnrollmentStatus {
        ENROLLED, IN_PROGRESS, COMPLETED, DROPPED, REVISIT_REQUESTED
    }
}
