package com.karmika.hrms.repository;

import com.karmika.hrms.entity.LeaveBalance;
import com.karmika.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByEmployee(Employee employee);

    Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndYear(
            Employee employee,
            com.karmika.hrms.entity.LeaveType leaveType,
            Integer year);
}
