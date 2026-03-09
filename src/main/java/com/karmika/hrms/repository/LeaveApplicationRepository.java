package com.karmika.hrms.repository;

import com.karmika.hrms.entity.LeaveApplication;
import com.karmika.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByEmployee(Employee employee);

    List<LeaveApplication> findByStatus(LeaveApplication.LeaveStatus status);

    List<LeaveApplication> findByEmployeeAndStatus(Employee employee, LeaveApplication.LeaveStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveApplication l WHERE l.status = 'APPROVED' AND :date BETWEEN l.startDate AND l.endDate")
    List<LeaveApplication> findApprovedLeavesOnDate(java.time.LocalDate date);
}
