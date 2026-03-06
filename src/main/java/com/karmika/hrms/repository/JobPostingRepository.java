package com.karmika.hrms.repository;

import com.karmika.hrms.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    List<JobPosting> findByStatus(JobPosting.JobStatus status);

    List<JobPosting> findByDepartmentId(Long departmentId);

    List<JobPosting> findByPostedByAndStatus(com.karmika.hrms.entity.Employee postedBy, JobPosting.JobStatus status);

    @Query("SELECT j FROM JobPosting j WHERE j.closingDate >= ?1 AND j.status = 'OPEN'")
    List<JobPosting> findActiveJobsByClosingDate(LocalDate date);

    @Query("SELECT j FROM JobPosting j WHERE LOWER(j.jobTitle) LIKE LOWER(CONCAT('%', ?1, '%')) OR LOWER(j.location) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<JobPosting> searchJobs(String keyword);

    List<JobPosting> findByLocationAndStatus(String location, JobPosting.JobStatus status);

    @Query("SELECT COUNT(j) FROM JobPosting j WHERE j.status = 'OPEN'")
    Long countOpenJobs();
}

