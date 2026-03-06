package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourseIdOrderByLessonOrderAsc(Long courseId);

    long countByCourseId(Long courseId);
}
