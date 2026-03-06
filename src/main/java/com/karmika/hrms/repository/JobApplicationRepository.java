package com.karmika.hrms.repository;

import com.karmika.hrms.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByJobPostingId(Long jobPostingId);

    List<JobApplication> findByStatus(JobApplication.ApplicationStatus status);

    List<JobApplication> findByCandidateEmail(String candidateEmail);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.jobPosting.id = ?1 AND ja.status = ?2")
    List<JobApplication> findByJobPostingAndStatus(Long jobPostingId, JobApplication.ApplicationStatus status);

    @Query("SELECT ja FROM JobApplication ja WHERE LOWER(ja.candidateName) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(ja.candidateEmail) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<JobApplication> searchApplications(String keyword);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.status = 'APPLIED' OR ja.status = 'SCREENING'")
    Long countPendingApplications();

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.jobPosting.id = ?1")
    Long countApplicationsForJob(Long jobPostingId);

    Optional<JobApplication> findByIdAndStatus(Long id, JobApplication.ApplicationStatus status);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.resumeValidated = false ORDER BY ja.createdAt ASC")
    List<JobApplication> findUnvalidatedResumes();

    @Query("SELECT ja FROM JobApplication ja WHERE ja.resumeQualityScore >= ?1 ORDER BY ja.resumeQualityScore DESC")
    List<JobApplication> findApplicationsByResumeScore(Integer minScore);
}

