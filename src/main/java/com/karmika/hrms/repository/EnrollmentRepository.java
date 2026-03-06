package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByEmployeeId(Long employeeId);

    List<Enrollment> findByCourseId(Long courseId);

    Optional<Enrollment> findByCourseIdAndEmployeeId(Long courseId, Long employeeId);

    boolean existsByCourseIdAndEmployeeId(Long courseId, Long employeeId);

    long countByCourseId(Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.employee.id = :empId AND e.status = 'COMPLETED'")
    long countCompletedByEmployee(@Param("empId") Long empId);
}
