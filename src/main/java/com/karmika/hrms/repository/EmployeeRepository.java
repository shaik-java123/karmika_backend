package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmail(String email);

    List<Employee> findByStatus(Employee.EmployeeStatus status);

    List<Employee> findByDepartmentId(Long departmentId);

    Boolean existsByEmployeeId(String employeeId);

    Boolean existsByEmail(String email);
}
