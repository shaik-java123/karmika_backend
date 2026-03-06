package com.karmika.hrms.repository;

import com.karmika.hrms.entity.InterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, Long> {

    List<InterviewFeedback> findByInterviewId(Long interviewId);

    List<InterviewFeedback> findByInterviewerId(Long interviewerId);

    Optional<InterviewFeedback> findByInterviewIdAndInterviewerId(Long interviewId, Long interviewerId);

    @Query("SELECT ifb FROM InterviewFeedback ifb WHERE ifb.recommendation IN ('STRONG_YES', 'YES')")
    List<InterviewFeedback> findPositiveFeedbacks();

    @Query("SELECT ifb FROM InterviewFeedback ifb WHERE ifb.rating >= ?1 ORDER BY ifb.rating DESC")
    List<InterviewFeedback> findByMinRating(Double minRating);

    @Query("SELECT COUNT(ifb) FROM InterviewFeedback ifb WHERE ifb.recommendation = 'STRONG_YES'")
    Long countStrongPositiveFeedbacks();
}

