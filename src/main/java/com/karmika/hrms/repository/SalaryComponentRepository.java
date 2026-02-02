package com.karmika.hrms.repository;

import com.karmika.hrms.entity.SalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, Long> {
    Optional<SalaryComponent> findByCode(String code);

    List<SalaryComponent> findByIsActiveTrue();

    List<SalaryComponent> findByType(SalaryComponent.ComponentType type);
}
