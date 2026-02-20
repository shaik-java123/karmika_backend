package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Appraisal;
import com.karmika.hrms.entity.AppraisalCycle;
import com.karmika.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppraisalRepository extends JpaRepository<Appraisal, Long> {

    List<Appraisal> findByCycle(AppraisalCycle cycle);

    List<Appraisal> findByEmployee(Employee employee);

    Optional<Appraisal> findByCycleAndEmployee(AppraisalCycle cycle, Employee employee);

    List<Appraisal> findByManager(Employee manager);

    List<Appraisal> findByStatus(Appraisal.AppraisalStatus status);

    List<Appraisal> findByCycleAndStatus(AppraisalCycle cycle, Appraisal.AppraisalStatus status);
}
