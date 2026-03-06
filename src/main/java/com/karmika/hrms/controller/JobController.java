package com.karmika.hrms.controller;

import com.karmika.hrms.dto.JobApplicationDTO;
import com.karmika.hrms.dto.JobPostingDTO;
import com.karmika.hrms.dto.ResumeValidationResultDTO;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.UserRepository;
import com.karmika.hrms.entity.User;
import com.karmika.hrms.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recruitment")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    // ==================== JOB POSTINGS ====================

    /**
     * Create a new job posting (HR/ADMIN only)
     * POST /api/recruitment/jobs
     */
    @PostMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> createJobPosting(@RequestBody JobPostingDTO dto, Authentication auth) {
        try {
            Employee postedBy = findEmployeeByAuth(auth);
            JobPostingDTO created = jobService.createJobPosting(dto, postedBy);
            return ResponseEntity
                    .ok(Map.of("success", true, "data", created, "message", "Job posting created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all job postings (all authenticated users)
     * GET /api/recruitment/jobs
     */
    @GetMapping("/jobs")
    public ResponseEntity<?> getAllJobPostings(@RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            List<JobPostingDTO> jobs;
            if (search != null && !search.isBlank()) {
                jobs = jobService.searchJobPostings(search);
            } else if (status != null && !status.equalsIgnoreCase("ALL")) {
                jobs = jobService.getJobPostingsByStatus(status);
            } else {
                jobs = jobService.getAllJobPostings();
            }
            return ResponseEntity.ok(Map.of("success", true, "data", jobs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get a specific job posting
     * GET /api/recruitment/jobs/{id}
     */
    @GetMapping("/jobs/{id}")
    public ResponseEntity<?> getJobPosting(@PathVariable Long id) {
        try {
            JobPostingDTO job = jobService.getJobPostingById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", job));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update a job posting (HR/ADMIN only)
     * PUT /api/recruitment/jobs/{id}
     */
    @PutMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> updateJobPosting(@PathVariable Long id, @RequestBody JobPostingDTO dto) {
        try {
            JobPostingDTO updated = jobService.updateJobPosting(id, dto);
            return ResponseEntity
                    .ok(Map.of("success", true, "data", updated, "message", "Job posting updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Publish a job posting (HR/ADMIN only)
     * POST /api/recruitment/jobs/{id}/publish
     */
    @PostMapping("/jobs/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> publishJobPosting(@PathVariable Long id) {
        try {
            JobPostingDTO updated = jobService.publishJobPosting(id);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "Job posting published"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Close a job posting (HR/ADMIN only)
     * POST /api/recruitment/jobs/{id}/close
     */
    @PostMapping("/jobs/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> closeJobPosting(@PathVariable Long id) {
        try {
            JobPostingDTO updated = jobService.closeJobPosting(id);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "Job posting closed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Put job posting on hold (HR/ADMIN only)
     * POST /api/recruitment/jobs/{id}/hold
     */
    @PostMapping("/jobs/{id}/hold")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> holdJobPosting(@PathVariable Long id) {
        try {
            JobPostingDTO updated = jobService.putOnHold(id);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "Job posting put on hold"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Delete a job posting (ADMIN only)
     * DELETE /api/recruitment/jobs/{id}
     */
    @DeleteMapping("/jobs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteJobPosting(@PathVariable Long id) {
        try {
            jobService.deleteJobPosting(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Job posting deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== APPLICATIONS ====================

    /**
     * Submit application for a job (public endpoint / all authenticated)
     * POST /api/recruitment/jobs/{jobId}/apply
     * Supports both JSON and multipart/form-data (for CV upload)
     */
    @PostMapping(value = "/jobs/{jobId}/apply", consumes = { "multipart/form-data" })
    public ResponseEntity<?> submitApplication(
            @PathVariable Long jobId,
            @RequestParam("candidateName") String candidateName,
            @RequestParam("candidateEmail") String candidateEmail,
            @RequestParam(value = "candidatePhone", required = false) String candidatePhone,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam(value = "totalExperienceYears", required = false) Integer totalExperienceYears,
            @RequestParam(value = "expectedCtc", required = false) java.math.BigDecimal expectedCtc,
            @RequestParam(value = "noticePeriodDays", required = false) Integer noticePeriodDays,
            @RequestParam(value = "resumeFile", required = false) org.springframework.web.multipart.MultipartFile resumeFile) {
        try {
            JobApplicationDTO dto = new JobApplicationDTO();
            dto.setCandidateName(candidateName);
            dto.setCandidateEmail(candidateEmail);
            dto.setCandidatePhone(candidatePhone);
            dto.setCoverLetter(coverLetter);
            dto.setTotalExperienceYears(totalExperienceYears);
            dto.setExpectedCtc(expectedCtc);
            dto.setNoticePeriodDays(noticePeriodDays);
            dto.setSource("PORTAL");

            JobApplicationDTO created = jobService.submitApplicationWithFile(jobId, dto, resumeFile);
            return ResponseEntity
                    .ok(Map.of("success", true, "data", created, "message", "Application submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping(value = "/jobs/{jobId}/apply", consumes = { "application/json" })
    public ResponseEntity<?> submitApplicationJson(@PathVariable Long jobId, @RequestBody JobApplicationDTO dto) {
        try {
            JobApplicationDTO created = jobService.submitApplication(jobId, dto);
            return ResponseEntity
                    .ok(Map.of("success", true, "data", created, "message", "Application submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all applications for a specific job (HR/ADMIN/MANAGER only)
     * GET /api/recruitment/jobs/{jobId}/applications
     */
    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> getApplicationsForJob(@PathVariable Long jobId) {
        try {
            List<JobApplicationDTO> apps = jobService.getApplicationsForJob(jobId);
            return ResponseEntity.ok(Map.of("success", true, "data", apps));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all applications (HR/ADMIN only)
     * GET /api/recruitment/applications
     */
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> getAllApplications(@RequestParam(required = false) String search) {
        try {
            List<JobApplicationDTO> apps;
            if (search != null && !search.isBlank()) {
                apps = jobService.searchApplications(search);
            } else {
                apps = jobService.getAllApplications();
            }
            return ResponseEntity.ok(Map.of("success", true, "data", apps));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get applications for the current logged in user
     * GET /api/recruitment/my-applications
     */
    @GetMapping("/my-applications")
    public ResponseEntity<?> getMyApplications(Authentication auth, @RequestParam(required = false) String email) {
        try {
            String targetEmail = email;

            // If authenticated, get email from employee profile or user account
            if (auth != null && auth.isAuthenticated()) {
                Employee emp = findEmployeeByAuth(auth);
                if (emp != null && emp.getEmail() != null) {
                    targetEmail = emp.getEmail();
                } else {
                    // Fallback to User email
                    userRepository.findByUsername(auth.getName()).ifPresent(u -> {
                        if (u.getEmail() != null) {
                            // We use a final array wrapper or just check the value
                        }
                    });
                    // Simpler:
                    User u = userRepository.findByUsername(auth.getName()).orElse(null);
                    if (u != null && u.getEmail() != null) {
                        targetEmail = u.getEmail();
                    }
                }
            }

            if (targetEmail == null || targetEmail.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Email is required to track applications"));
            }

            List<JobApplicationDTO> apps = jobService.getApplicationsByEmail(targetEmail);
            return ResponseEntity.ok(Map.of("success", true, "data", apps));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get specific application detail (HR/ADMIN only)
     * GET /api/recruitment/applications/{id}
     */
    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> getApplication(@PathVariable Long id) {
        try {
            JobApplicationDTO app = jobService.getApplicationById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", app));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update application status (HR/ADMIN/MANAGER only)
     * PUT /api/recruitment/applications/{id}/status
     */
    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Status is required"));
            }
            JobApplicationDTO updated = jobService.updateApplicationStatus(id, status);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "Application status updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== RESUME VALIDATION ====================

    /**
     * Validate resume content against job requirements
     * POST /api/recruitment/resume/validate
     */
    @PostMapping("/resume/validate")
    public ResponseEntity<?> validateResume(@RequestBody Map<String, Object> body) {
        try {
            String resumeContent = (String) body.get("resumeContent");
            if (resumeContent == null || resumeContent.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Resume content is required"));
            }

            int minExp = 0;
            List<String> requiredSkills = List.of();

            if (body.containsKey("minExperienceYears")) {
                Object exp = body.get("minExperienceYears");
                if (exp instanceof Integer i)
                    minExp = i;
                else if (exp instanceof String s)
                    minExp = Integer.parseInt(s);
            }

            if (body.containsKey("requiredSkills")) {
                Object skills = body.get("requiredSkills");
                if (skills instanceof List<?> l) {
                    requiredSkills = l.stream().map(Object::toString).toList();
                } else if (skills instanceof String s) {
                    requiredSkills = List.of(s.split(",")).stream().map(String::trim).filter(sk -> !sk.isEmpty())
                            .toList();
                }
            }

            ResumeValidationResultDTO result = jobService.validateResumeContent(resumeContent, minExp, requiredSkills);
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Validate resume for a specific application (HR/ADMIN only)
     * POST /api/recruitment/applications/{id}/validate-resume
     */
    @PostMapping("/applications/{id}/validate-resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> validateResumeForApplication(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String resumeContent = body.get("resumeContent");
            if (resumeContent == null || resumeContent.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Resume content is required"));
            }
            ResumeValidationResultDTO result = jobService.validateAndUpdateApplication(id, resumeContent);
            return ResponseEntity
                    .ok(Map.of("success", true, "data", result, "message", "Resume validated and application updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get unvalidated resumes (HR/ADMIN only)
     * GET /api/recruitment/resume/unvalidated
     */
    @GetMapping("/resume/unvalidated")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> getUnvalidatedResumes() {
        try {
            List<JobApplicationDTO> apps = jobService.getUnvalidatedResumes();
            return ResponseEntity.ok(Map.of("success", true, "data", apps));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get top candidates by resume score (HR/ADMIN only)
     * GET /api/recruitment/applications/top-candidates?minScore=70
     */
    @GetMapping("/applications/top-candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> getTopCandidates(@RequestParam(defaultValue = "70") int minScore) {
        try {
            List<JobApplicationDTO> apps = jobService.getTopCandidatesByScore(minScore);
            return ResponseEntity.ok(Map.of("success", true, "data", apps));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== STATS ====================

    /**
     * Get recruitment statistics (HR/ADMIN only)
     * GET /api/recruitment/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> getRecruitmentStats() {
        try {
            Map<String, Object> stats = jobService.getRecruitmentStats();
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== HELPERS ====================

    private Employee findEmployeeByAuth(Authentication auth) {
        if (auth == null)
            return null;
        String username = auth.getName();
        return employeeRepository.findAll().stream()
                .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
}
