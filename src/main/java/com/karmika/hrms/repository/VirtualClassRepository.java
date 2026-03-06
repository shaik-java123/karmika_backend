package com.karmika.hrms.repository;

import com.karmika.hrms.entity.VirtualClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VirtualClassRepository extends JpaRepository<VirtualClass, Long> {

    List<VirtualClass> findByCourseIdOrderByScheduledAtAsc(Long courseId);

    List<VirtualClass> findByScheduledAtAfterOrderByScheduledAtAsc(LocalDateTime after);

    List<VirtualClass> findByHostIdOrderByScheduledAtDesc(Long hostId);
}
