package com.karmika.hrms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karmika.hrms.dto.JobApplicationDTO;
import com.karmika.hrms.dto.JobPostingDTO;
import com.karmika.hrms.dto.ResumeValidationResultDTO;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.JobApplication;
import com.karmika.hrms.entity.JobPosting;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.JobApplicationRepository;
import com.karmika.hrms.repository.JobPostingRepository;
import com.karmika.hrms.util.ResumeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class JobService {

    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final EmployeeRepository employeeRepository;
    private final ResumeValidator resumeValidator;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    // ===================== JOB POSTING CRUD =====================

    public JobPostingDTO createJobPosting(JobPostingDTO dto, Employee postedBy) {
        JobPosting posting = new JobPosting();
        mapDtoToEntity(dto, posting);
        posting.setPostedDate(LocalDate.now());
        posting.setStatus(JobPosting.JobStatus.OPEN);
        if (postedBy != null) {
            posting.setPostedBy(postedBy);
        }
        return mapToDTO(jobPostingRepository.save(posting));
    }

    @Transactional(readOnly = true)
    public List<JobPostingDTO> getAllJobPostings() {
        return jobPostingRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobPostingDTO> getJobPostingsByStatus(String status) {
        try {
            JobPosting.JobStatus jobStatus = JobPosting.JobStatus.valueOf(status.toUpperCase());
            return jobPostingRepository.findByStatus(jobStatus).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return getAllJobPostings();
        }
    }

    @Transactional(readOnly = true)
    public JobPostingDTO getJobPostingById(Long id) {
        JobPosting posting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job posting not found with id: " + id));
        return mapToDTO(posting);
    }

    @Transactional(readOnly = true)
    public List<JobPostingDTO> searchJobPostings(String keyword) {
        return jobPostingRepository.searchJobs(keyword).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public JobPostingDTO updateJobPosting(Long id, JobPostingDTO dto) {
        JobPosting posting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job posting not found with id: " + id));
        mapDtoToEntity(dto, posting);
        return mapToDTO(jobPostingRepository.save(posting));
    }

    public JobPostingDTO publishJobPosting(Long id) {
        JobPosting posting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job posting not found with id: " + id));
        posting.setStatus(JobPosting.JobStatus.OPEN);
        posting.setPostedDate(LocalDate.now());
        return mapToDTO(jobPostingRepository.save(posting));
    }

    public JobPostingDTO closeJobPosting(Long id) {
        JobPosting posting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job posting not found with id: " + id));
        posting.setStatus(JobPosting.JobStatus.CLOSED);
        return mapToDTO(jobPostingRepository.save(posting));
    }

    public JobPostingDTO putOnHold(Long id) {
        JobPosting posting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job posting not found with id: " + id));
        posting.setStatus(JobPosting.JobStatus.ON_HOLD);
        return mapToDTO(jobPostingRepository.save(posting));
    }

    public void deleteJobPosting(Long id) {
        if (!jobPostingRepository.existsById(id)) {
            throw new RuntimeException("Job posting not found with id: " + id);
        }
        jobPostingRepository.deleteById(id);
    }

    // ===================== APPLICATION MANAGEMENT =====================

    public JobApplicationDTO submitApplication(Long jobId, JobApplicationDTO dto) {
        JobPosting posting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job posting not found with id: " + jobId));

        if (posting.getStatus() != JobPosting.JobStatus.OPEN) {
            throw new RuntimeException("Job posting is not open for applications");
        }

        JobApplication application = new JobApplication();
        application.setJobPosting(posting);
        application.setCandidateName(dto.getCandidateName());
        application.setCandidateEmail(dto.getCandidateEmail());
        application.setCandidatePhone(dto.getCandidatePhone());
        application.setResumeUrl(dto.getResumeUrl());
        application.setCoverLetter(dto.getCoverLetter());
        application.setExpectedCtc(dto.getExpectedCtc());
        application.setNoticePeriodDays(dto.getNoticePeriodDays());
        application.setTotalExperienceYears(dto.getTotalExperienceYears());
        application.setSource(dto.getSource());
        application.setAppliedDate(LocalDate.now());
        application.setStatus(JobApplication.ApplicationStatus.APPLIED);

        // Auto-validate resume if content provided
        if (dto.getResumeUrl() != null && !dto.getResumeUrl().isEmpty()) {
            application.setResumeValidated(false);
        }

        return mapApplicationToDTO(jobApplicationRepository.save(application));
    }

    public JobApplicationDTO submitApplicationWithFile(Long jobId, JobApplicationDTO dto,
            org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.saveFile(file, "recruitment/resumes/" + jobId);
            dto.setResumeUrl(path);
        }
        return submitApplication(jobId, dto);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationDTO> getApplicationsForJob(Long jobId) {
        return jobApplicationRepository.findByJobPostingId(jobId).stream()
                .map(this::mapApplicationToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobApplicationDTO> getApplicationsByEmail(String email) {
        return jobApplicationRepository.findByCandidateEmail(email).stream()
                .map(this::mapApplicationToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobApplicationDTO> getAllApplications() {
        return jobApplicationRepository.findAll().stream()
                .map(this::mapApplicationToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobApplicationDTO getApplicationById(Long id) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + id));
        return mapApplicationToDTO(application);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationDTO> searchApplications(String keyword) {
        return jobApplicationRepository.searchApplications(keyword).stream()
                .map(this::mapApplicationToDTO)
                .collect(Collectors.toList());
    }

    public JobApplicationDTO updateApplicationStatus(Long appId, String status) {
        JobApplication application = jobApplicationRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + appId));
        try {
            application.setStatus(JobApplication.ApplicationStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
        return mapApplicationToDTO(jobApplicationRepository.save(application));
    }

    // ===================== RESUME VALIDATION =====================

    public ResumeValidationResultDTO validateResumeContent(String resumeContent,
            int minExperienceYears,
            List<String> requiredSkills) {
        ResumeValidator.JobRequirements requirements = new ResumeValidator.JobRequirements(minExperienceYears,
                requiredSkills);
        ResumeValidationResultDTO result = resumeValidator.validateResume(resumeContent, requirements);
        result.setValidatedAt(LocalDateTime.now());
        return result;
    }

    public ResumeValidationResultDTO validateAndUpdateApplication(Long applicationId,
            String resumeContent) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found with id: " + applicationId));

        JobPosting posting = application.getJobPosting();

        // Build requirements from job posting
        List<String> requiredSkills = List.of();
        if (posting.getRequirements() != null && !posting.getRequirements().isEmpty()) {
            // Extract skills from requirements text (simple comma-split approach)
            requiredSkills = Arrays.stream(posting.getRequirements().split("[,\\n]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.length() < 30)
                    .collect(Collectors.toList());
        }

        int minExp = posting.getExperienceMin() != null ? posting.getExperienceMin() : 0;
        ResumeValidator.JobRequirements requirements = new ResumeValidator.JobRequirements(minExp, requiredSkills);

        ResumeValidationResultDTO result = resumeValidator.validateResume(resumeContent, requirements);

        // Update the application with validation results
        application.setResumeValidated(true);
        application.setResumeQualityScore(result.getQualityScore());
        application.setResumeSummary(result.getResumeSummary());
        application.setResumeValidationErrors(result.getValidationErrors());
        application.setRequiredSkillsMatch(result.getRequiredSkillsMatch());
        application.setAdditionalSkillsFound(result.getAdditionalSkillsFound());

        jobApplicationRepository.save(application);

        result.setApplicationId(applicationId);
        result.setCandidateName(application.getCandidateName());
        result.setCandidateEmail(application.getCandidateEmail());
        result.setValidatedAt(LocalDateTime.now());

        return result;
    }

    @Transactional(readOnly = true)
    public List<JobApplicationDTO> getUnvalidatedResumes() {
        return jobApplicationRepository.findUnvalidatedResumes().stream()
                .map(this::mapApplicationToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JobApplicationDTO> getTopCandidatesByScore(int minScore) {
        return jobApplicationRepository.findApplicationsByResumeScore(minScore).stream()
                .map(this::mapApplicationToDTO)
                .collect(Collectors.toList());
    }

    // ===================== STATS =====================

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getRecruitmentStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalOpenJobs", jobPostingRepository.countOpenJobs());
        stats.put("totalApplications", jobApplicationRepository.count());
        stats.put("pendingApplications", jobApplicationRepository.countPendingApplications());
        stats.put("totalJobPostings", jobPostingRepository.count());
        return stats;
    }

    // ===================== MAPPERS =====================

    private void mapDtoToEntity(JobPostingDTO dto, JobPosting posting) {
        posting.setJobTitle(dto.getJobTitle());
        posting.setJobDescription(dto.getJobDescription());
        posting.setRequirements(dto.getRequirements());
        posting.setLocation(dto.getLocation());
        posting.setExperienceMin(dto.getExperienceMin());
        posting.setExperienceMax(dto.getExperienceMax());
        posting.setSalaryMin(dto.getSalaryMin());
        posting.setSalaryMax(dto.getSalaryMax());
        posting.setNumberOfOpenings(dto.getNumberOfOpenings() != null ? dto.getNumberOfOpenings() : 1);
        posting.setClosingDate(dto.getClosingDate());

        if (dto.getEmploymentType() != null) {
            try {
                posting.setEmploymentType(JobPosting.EmploymentType.valueOf(dto.getEmploymentType().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (dto.getStatus() != null) {
            try {
                posting.setStatus(JobPosting.JobStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (dto.getDepartmentId() != null) {
            // department join - using optional lookup
        }
    }

    private JobPostingDTO mapToDTO(JobPosting posting) {
        JobPostingDTO dto = new JobPostingDTO();
        dto.setId(posting.getId());
        dto.setJobTitle(posting.getJobTitle());
        dto.setJobDescription(posting.getJobDescription());
        dto.setRequirements(posting.getRequirements());
        dto.setLocation(posting.getLocation());
        dto.setExperienceMin(posting.getExperienceMin());
        dto.setExperienceMax(posting.getExperienceMax());
        dto.setSalaryMin(posting.getSalaryMin());
        dto.setSalaryMax(posting.getSalaryMax());
        dto.setNumberOfOpenings(posting.getNumberOfOpenings());
        dto.setClosingDate(posting.getClosingDate());
        dto.setPostedDate(posting.getPostedDate());
        dto.setStatus(posting.getStatus() != null ? posting.getStatus().name() : null);
        dto.setEmploymentType(posting.getEmploymentType() != null ? posting.getEmploymentType().name() : null);
        dto.setCreatedAt(posting.getCreatedAt());
        dto.setUpdatedAt(posting.getUpdatedAt());

        if (posting.getDepartment() != null) {
            dto.setDepartmentId(posting.getDepartment().getId());
            dto.setDepartmentName(posting.getDepartment().getName());
        }

        if (posting.getPostedBy() != null) {
            dto.setPostedBy(posting.getPostedBy().getId());
            dto.setPostedByName(posting.getPostedBy().getFirstName() + " " + posting.getPostedBy().getLastName());
        }

        // Count applications
        Long appCount = jobApplicationRepository.countApplicationsForJob(posting.getId());
        dto.setApplicationsCount(appCount);

        return dto;
    }

    private JobApplicationDTO mapApplicationToDTO(JobApplication app) {
        JobApplicationDTO dto = new JobApplicationDTO();
        dto.setId(app.getId());
        dto.setCandidateName(app.getCandidateName());
        dto.setCandidateEmail(app.getCandidateEmail());
        dto.setCandidatePhone(app.getCandidatePhone());
        dto.setResumeUrl(app.getResumeUrl());
        dto.setCoverLetter(app.getCoverLetter());
        dto.setCurrentCtc(app.getCurrentCtc());
        dto.setExpectedCtc(app.getExpectedCtc());
        dto.setNoticePeriodDays(app.getNoticePeriodDays());
        dto.setTotalExperienceYears(app.getTotalExperienceYears());
        dto.setStatus(app.getStatus() != null ? app.getStatus().name() : null);
        dto.setAppliedDate(app.getAppliedDate());
        dto.setSource(app.getSource());
        dto.setResumeValidated(app.getResumeValidated());
        dto.setResumeValidationErrors(app.getResumeValidationErrors());
        dto.setResumeSummary(app.getResumeSummary());
        dto.setResumeQualityScore(app.getResumeQualityScore());
        dto.setRequiredSkillsMatch(app.getRequiredSkillsMatch());
        dto.setAdditionalSkillsFound(app.getAdditionalSkillsFound());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());

        if (app.getJobPosting() != null) {
            dto.setJobPostingId(app.getJobPosting().getId());
            dto.setJobTitle(app.getJobPosting().getJobTitle());
        }

        return dto;
    }
}
