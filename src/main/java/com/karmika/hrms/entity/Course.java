package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lms_courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String category; // e.g. "Technical", "Leadership", "Compliance", "Soft Skills"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level = Level.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Column(length = 500)
    private String tags; // comma-separated tags

    private Integer durationHours; // estimated total duration in hours

    private Boolean certificateEnabled = false;

    @ManyToOne
    @JoinColumn(name = "instructor_id")
    private Employee instructor;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Employee createdBy;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lessonOrder ASC")
    private List<Lesson> lessons = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("scheduledAt ASC")
    private List<VirtualClass> virtualClasses = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    public enum Level {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public enum CourseStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
