package com.karmika.hrms.repository;

import com.karmika.hrms.entity.AppraisalCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppraisalCycleRepository extends JpaRepository<AppraisalCycle, Long> {

    List<AppraisalCycle> findByStatus(AppraisalCycle.CycleStatus status);

    Optional<AppraisalCycle> findByCycleName(String cycleName);

    List<AppraisalCycle> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate date1, LocalDate date2);
}
