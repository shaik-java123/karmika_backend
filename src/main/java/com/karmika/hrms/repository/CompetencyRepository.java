package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Competency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetencyRepository extends JpaRepository<Competency, Long> {

    List<Competency> findByIsActiveTrueOrderByDisplayOrder();

    List<Competency> findByCategory(Competency.CompetencyCategory category);

    Optional<Competency> findByCode(String code);
}
