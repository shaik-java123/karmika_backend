package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.SalarySlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalarySlipRepository extends JpaRepository<SalarySlip, Long> {
    List<SalarySlip> findByEmployee(Employee employee);

    Optional<SalarySlip> findByEmployeeAndMonthAndYear(Employee employee, Integer month, Integer year);

    List<SalarySlip> findByMonthAndYear(Integer month, Integer year);

    List<SalarySlip> findByPaymentStatus(SalarySlip.PaymentStatus status);
}
