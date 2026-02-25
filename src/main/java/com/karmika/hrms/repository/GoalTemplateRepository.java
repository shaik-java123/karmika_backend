package com.karmika.hrms.repository;

import com.karmika.hrms.entity.AppraisalCycle;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.GoalTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalTemplateRepository extends JpaRepository<GoalTemplate, Long> {

    List<GoalTemplate> findByManagerOrderByCreatedAtDesc(Employee manager);

    List<GoalTemplate> findByCycleOrderByCreatedAtDesc(AppraisalCycle cycle);

    Optional<GoalTemplate> findByManagerAndCycle(Employee manager, AppraisalCycle cycle);

    @Query("SELECT t FROM GoalTemplate t WHERE t.cycle = :cycle AND t.status = 'PUBLISHED'")
    List<GoalTemplate> findPublishedByCycle(@Param("cycle") AppraisalCycle cycle);

    @Query("SELECT t FROM GoalTemplate t WHERE t.manager.id = :managerId ORDER BY t.createdAt DESC")
    List<GoalTemplate> findByManagerId(@Param("managerId") Long managerId);
}
