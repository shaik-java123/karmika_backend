package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByUser(User user);

    List<Employee> findByStatus(Employee.EmployeeStatus status);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByReportingManager(Employee manager);

    Boolean existsByEmployeeId(String employeeId);

    Boolean existsByEmail(String email);
}
