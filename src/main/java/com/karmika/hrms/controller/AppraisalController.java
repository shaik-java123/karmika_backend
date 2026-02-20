package com.karmika.hrms.controller;

import com.karmika.hrms.dto.AppraisalDTO;
import com.karmika.hrms.entity.*;
import com.karmika.hrms.repository.*;
import com.karmika.hrms.service.AppraisalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appraisals")
@RequiredArgsConstructor
public class AppraisalController {

    private final AppraisalService appraisalService;
    private final AppraisalCycleRepository cycleRepository;
    private final AppraisalRepository appraisalRepository;
    private final AppraisalReviewRepository reviewRepository;
    private final AppraisalRatingRepository ratingRepository;
    private final CompetencyRepository competencyRepository;
    private final EmployeeRepository employeeRepository;

    // ==================== CYCLE MANAGEMENT ====================

    /**
     * Create a new appraisal cycle (ADMIN/HR only)
     */
    @PostMapping("/cycles")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> createCycle(@RequestBody AppraisalCycle cycle, Authentication auth) {
        try {
            Employee creator = getEmployeeFromAuth(auth);
            AppraisalCycle created = appraisalService.createCycle(cycle, creator);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Appraisal cycle created successfully",
                    "cycle", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get all appraisal cycles
     */
    @GetMapping("/cycles")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> getAllCycles() {
        try {
            List<AppraisalCycle> cycles = cycleRepository.findAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cycles", cycles));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get active appraisal cycles
     */
    @GetMapping("/cycles/active")
    public ResponseEntity<?> getActiveCycles() {
        try {
            List<AppraisalCycle> cycles = cycleRepository.findByStatus(AppraisalCycle.CycleStatus.ACTIVE);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cycles", cycles));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Activate a cycle (ADMIN/HR only)
     */
    @PostMapping("/cycles/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> activateCycle(@PathVariable Long id) {
        try {
            appraisalService.activateCycle(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cycle activated and appraisals created for all employees"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    // ==================== COMPETENCY MANAGEMENT ====================

    /**
     * Create a competency (ADMIN/HR only)
     */
    @PostMapping("/competencies")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> createCompetency(@RequestBody Competency competency) {
        try {
            Competency created = competencyRepository.save(competency);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Competency created successfully",
                    "competency", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get all active competencies
     */
    @GetMapping("/competencies")
    public ResponseEntity<?> getAllCompetencies() {
        try {
            List<Competency> competencies = competencyRepository.findByIsActiveTrueOrderByDisplayOrder();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "competencies", competencies));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    // ==================== APPRAISAL MANAGEMENT ====================

    /**
     * Get my appraisals (for current user)
     */
    @GetMapping("/my-appraisals")
    public ResponseEntity<?> getMyAppraisals(Authentication auth) {
        try {
            Employee employee = getEmployeeFromAuth(auth);
            List<Appraisal> appraisals = appraisalRepository.findByEmployee(employee);
            List<AppraisalDTO> dtos = appraisals.stream()
                    .map(appraisalService::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "appraisals", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get appraisals for a specific cycle (ADMIN/HR/MANAGER)
     */
    @GetMapping("/cycles/{cycleId}/appraisals")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> getCycleAppraisals(@PathVariable Long cycleId, Authentication auth) {
        try {
            AppraisalCycle cycle = cycleRepository.findById(cycleId)
                    .orElseThrow(() -> new RuntimeException("Cycle not found"));

            List<Appraisal> appraisals = appraisalRepository.findByCycle(cycle);

            // If manager, filter to show only their team
            if (isManager(auth) && !isAdminOrHR(auth)) {
                Employee manager = getEmployeeFromAuth(auth);
                appraisals = appraisals.stream()
                        .filter(a -> a.getManager() != null && a.getManager().getId().equals(manager.getId()))
                        .collect(Collectors.toList());
            }

            List<AppraisalDTO> dtos = appraisals.stream()
                    .map(appraisalService::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "appraisals", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get appraisal details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAppraisal(@PathVariable Long id, Authentication auth) {
        try {
            Appraisal appraisal = appraisalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appraisal not found"));

            // Check access
            Employee currentUser = getEmployeeFromAuth(auth);
            if (!canAccessAppraisal(appraisal, currentUser, auth)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Access denied"));
            }

            AppraisalDTO dto = appraisalService.convertToDTO(appraisal);

            // Get reviews
            List<AppraisalReview> reviews = reviewRepository.findByAppraisal(appraisal);
            dto.setReviews(reviews.stream()
                    .map(this::convertReviewToDTO)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "appraisal", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Add peer reviewers to an appraisal
     */
    @PostMapping("/{id}/peer-reviewers")
    public ResponseEntity<?> addPeerReviewers(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request,
            Authentication auth) {
        try {
            Appraisal appraisal = appraisalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appraisal not found"));

            // Only employee or HR/Admin can add peer reviewers
            Employee currentUser = getEmployeeFromAuth(auth);
            if (!appraisal.getEmployee().getId().equals(currentUser.getId()) && !isAdminOrHR(auth)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Access denied"));
            }

            List<Long> peerIds = request.get("peerIds");
            appraisalService.addPeerReviewers(id, peerIds);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Peer reviewers added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    // ==================== REVIEW SUBMISSION ====================

    /**
     * Get my pending reviews
     */
    @GetMapping("/my-reviews/pending")
    public ResponseEntity<?> getMyPendingReviews(Authentication auth) {
        try {
            Employee reviewer = getEmployeeFromAuth(auth);
            List<AppraisalReview> reviews = reviewRepository.findByReviewerAndStatus(
                    reviewer, AppraisalReview.ReviewStatus.PENDING);

            List<Map<String, Object>> reviewData = reviews.stream()
                    .map(r -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("reviewId", r.getId());
                        data.put("reviewerType", r.getReviewerType().toString());
                        data.put("employeeName", r.getAppraisal().getEmployee().getFirstName() + " " +
                                r.getAppraisal().getEmployee().getLastName());
                        data.put("cycleName", r.getAppraisal().getCycle().getCycleName());
                        data.put("status", r.getStatus().toString());
                        return data;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reviews", reviewData));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get review details for submission
     */
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<?> getReviewDetails(@PathVariable Long reviewId, Authentication auth) {
        try {
            AppraisalReview review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));

            // Check if current user is the reviewer
            Employee currentUser = getEmployeeFromAuth(auth);
            if (!review.getReviewer().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Access denied"));
            }

            // Get competencies
            List<Competency> competencies = competencyRepository.findByIsActiveTrueOrderByDisplayOrder();

            // Get existing ratings if any
            List<AppraisalRating> existingRatings = ratingRepository.findByReview(review);

            Map<String, Object> data = new HashMap<>();
            data.put("review", convertReviewToDTO(review));
            data.put("competencies", competencies);
            data.put("existingRatings", existingRatings);
            data.put("employee", review.getAppraisal().getEmployee());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Submit a review
     */
    @PostMapping("/reviews/{reviewId}/submit")
    public ResponseEntity<?> submitReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            AppraisalReview review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found"));

            // Check if current user is the reviewer
            Employee currentUser = getEmployeeFromAuth(auth);
            if (!review.getReviewer().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "Access denied"));
            }

            String overallComments = (String) request.get("overallComments");
            String strengths = (String) request.get("strengths");
            String areasOfImprovement = (String) request.get("areasOfImprovement");

            @SuppressWarnings("unchecked")
            Map<String, Integer> competencyRatings = (Map<String, Integer>) request.get("competencyRatings");

            @SuppressWarnings("unchecked")
            Map<String, String> competencyComments = (Map<String, String>) request.get("competencyComments");

            // Convert string keys to Long
            Map<Long, Integer> ratings = new HashMap<>();
            Map<Long, String> comments = new HashMap<>();

            if (competencyRatings != null) {
                for (Map.Entry<String, Integer> entry : competencyRatings.entrySet()) {
                    ratings.put(Long.parseLong(entry.getKey()), entry.getValue());
                }
            }

            if (competencyComments != null) {
                for (Map.Entry<String, String> entry : competencyComments.entrySet()) {
                    comments.put(Long.parseLong(entry.getKey()), entry.getValue());
                }
            }

            appraisalService.submitReview(reviewId, overallComments, strengths,
                    areasOfImprovement, ratings, comments);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    // ==================== APPROVAL ====================

    /**
     * Approve an appraisal (ADMIN/HR only)
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> approveAppraisal(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request,
            Authentication auth) {
        try {
            Employee approver = getEmployeeFromAuth(auth);
            String remarks = request != null ? request.get("remarks") : null;

            appraisalService.approveAppraisal(id, approver, remarks);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Appraisal approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    // ==================== HELPER METHODS ====================

    private Employee getEmployeeFromAuth(Authentication auth) {
        String username = auth.getName();
        return employeeRepository.findAll().stream()
                .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    private boolean isAdminOrHR(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_HR"));
    }

    private boolean isManager(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }

    private boolean canAccessAppraisal(Appraisal appraisal, Employee currentUser, Authentication auth) {
        // Admin/HR can access all
        if (isAdminOrHR(auth)) {
            return true;
        }

        // Employee can access their own
        if (appraisal.getEmployee().getId().equals(currentUser.getId())) {
            return true;
        }

        // Manager can access their team's appraisals
        if (appraisal.getManager() != null && appraisal.getManager().getId().equals(currentUser.getId())) {
            return true;
        }

        // Reviewers can access
        List<AppraisalReview> reviews = reviewRepository.findByAppraisal(appraisal);
        return reviews.stream().anyMatch(r -> r.getReviewer().getId().equals(currentUser.getId()));
    }

    private com.karmika.hrms.dto.AppraisalReviewDTO convertReviewToDTO(AppraisalReview review) {
        return com.karmika.hrms.dto.AppraisalReviewDTO.builder()
                .id(review.getId())
                .appraisalId(review.getAppraisal().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName())
                .reviewerType(review.getReviewerType().toString())
                .status(review.getStatus().toString())
                .overallComments(review.getOverallComments())
                .strengths(review.getStrengths())
                .areasOfImprovement(review.getAreasOfImprovement())
                .overallRating(review.getOverallRating())
                .submittedAt(review.getSubmittedAt() != null ? review.getSubmittedAt().toString() : null)
                .isAnonymous(review.getIsAnonymous())
                .build();
    }
}
