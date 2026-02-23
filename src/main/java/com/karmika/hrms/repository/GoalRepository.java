package com.karmika.hrms.repository;

import com.karmika.hrms.entity.AppraisalCycle;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    // All goals assigned to a specific employee
    List<Goal> findByAssignedToOrderByDueDateAsc(Employee employee);

    // Goals for a manager's direct reports
    List<Goal> findByAssignedByOrderByCreatedAtDesc(Employee assignedBy);

    // Goals for a cycle
    List<Goal> findByCycleOrderByDueDateAsc(AppraisalCycle cycle);

    // Goals for an employee in a specific cycle
    List<Goal> findByAssignedToAndCycleOrderByDueDateAsc(Employee employee, AppraisalCycle cycle);

    // All goals for a cycle grouped by employee (for managers/HR to see team
    // summary)
    @Query("SELECT g FROM Goal g WHERE g.cycle = :cycle AND g.assignedTo.reportingManager = :manager ORDER BY g.assignedTo.firstName, g.dueDate")
    List<Goal> findByCycleAndTeamManager(@Param("cycle") AppraisalCycle cycle, @Param("manager") Employee manager);

    // Count by status for dashboard
    Long countByAssignedToAndStatus(Employee employee, Goal.GoalStatus status);

    // Search by employee
    @Query("SELECT g FROM Goal g WHERE g.assignedTo.id = :empId ORDER BY g.createdAt DESC")
    List<Goal> findAllByEmployeeId(@Param("empId") Long empId);
}
