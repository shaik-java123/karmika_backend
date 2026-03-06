package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(Course.CourseStatus status);

    List<Course> findByCategoryIgnoreCase(String category);

    List<Course> findByInstructorId(Long instructorId);

    @Query("SELECT c FROM Course c WHERE " +
            "LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.category) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Course> search(@Param("q") String q);

    @Query("SELECT DISTINCT c.category FROM Course c ORDER BY c.category")
    List<String> findAllCategories();
}
