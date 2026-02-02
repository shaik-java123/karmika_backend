package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.EmployeeSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeSalaryStructureRepository extends JpaRepository<EmployeeSalaryStructure, Long> {
    List<EmployeeSalaryStructure> findByEmployee(Employee employee);

    List<EmployeeSalaryStructure> findByEmployeeAndIsActiveTrue(Employee employee);

    void deleteByEmployee(Employee employee);
}
