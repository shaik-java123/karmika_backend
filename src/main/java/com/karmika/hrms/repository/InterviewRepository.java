package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByJobApplicationId(Long jobApplicationId);

    List<Interview> findByStatus(Interview.InterviewStatus status);

    List<Interview> findByInterviewerIdAndStatus(Long interviewerId, Interview.InterviewStatus status);

    @Query("SELECT i FROM Interview i WHERE i.scheduledDate BETWEEN ?1 AND ?2 ORDER BY i.scheduledDate ASC")
    List<Interview> findScheduledInterviews(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT i FROM Interview i WHERE i.interviewer.id = ?1 AND i.scheduledDate >= ?2 ORDER BY i.scheduledDate ASC")
    List<Interview> findUpcomingInterviewsForInterviewer(Long interviewerId, LocalDateTime fromDate);

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.status = 'COMPLETED'")
    Long countCompletedInterviews();

    @Query("SELECT COUNT(i) FROM Interview i WHERE i.status = 'SCHEDULED'")
    Long countScheduledInterviews();
}

