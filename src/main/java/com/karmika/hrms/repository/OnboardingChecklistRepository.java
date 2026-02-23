package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.OnboardingChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OnboardingChecklistRepository extends JpaRepository<OnboardingChecklist, Long> {

    List<OnboardingChecklist> findByEmployeeOrderByCreatedAtAsc(Employee employee);

    List<OnboardingChecklist> findByEmployeeIdOrderByCreatedAtAsc(Long employeeId);

    long countByEmployeeIdAndStatus(Long employeeId, OnboardingChecklist.ChecklistStatus status);

    long countByEmployeeId(Long employeeId);
}
