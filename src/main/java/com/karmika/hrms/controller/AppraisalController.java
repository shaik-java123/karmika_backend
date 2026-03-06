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
    private final GoalRepository goalRepository;

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

    /**
     * Delete a cycle (ADMIN/HR only)
     */
    @DeleteMapping("/cycles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> deleteCycle(@PathVariable Long id) {
        try {
            appraisalService.deleteCycle(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cycle deleted successfully"));
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
     * Delete a competency (ADMIN/HR only)
     */
    @DeleteMapping("/competencies/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> deleteCompetency(@PathVariable Long id) {
        try {
            appraisalService.deleteCompetency(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Competency deleted successfully"));
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
                    .toList();

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
                        .toList();
            }

            List<AppraisalDTO> dtos = appraisals.stream()
                    .map(appraisalService::convertToDTO)
                    .toList();

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
            List<AppraisalReview> reviews = reviewRepository.findByReviewerAndStatusIn(
                    reviewer,
                    Arrays.asList(AppraisalReview.ReviewStatus.PENDING, AppraisalReview.ReviewStatus.IN_PROGRESS));

            List<Map<String, Object>> reviewData = reviews.stream()
                    .filter(r -> {
                        // For SELF reviews, they are always visible.
                        if (r.getReviewerType() == AppraisalReview.ReviewerType.SELF) {
                            return true;
                        }

                        Appraisal app = r.getAppraisal();

                        // Condition 1: Has the employee submitted their own self-review?
                        boolean selfCompleted = Optional.ofNullable(app.getSelfReviewCompleted()).orElse(false);

                        // Condition 2: Check goal status (at least one goal NOT in NOT_STARTED)
                        // This indicates they have started/submitted progress on goals.
                        boolean goalsStarted = false;
                        Employee reviewee = app.getEmployee();
                        AppraisalCycle cycle = app.getCycle();
                        List<Goal> goals = goalRepository.findByAssignedToAndCycleOrderByDueDateAsc(reviewee, cycle);

                        if (goals != null && !goals.isEmpty()) {
                            goalsStarted = goals.stream().anyMatch(g -> g.getStatus() != Goal.GoalStatus.NOT_STARTED);
                        }

                        // If they have completed self review OR started executing goals, the
                        // manager/peer can see it
                        return selfCompleted || goalsStarted;
                    })
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
                    .toList();

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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
            AppraisalCycle cycle = review.getAppraisal().getCycle();
            List<Competency> competencies;
            if (cycle.getCompetencies() != null && !cycle.getCompetencies().isEmpty()) {
                competencies = new ArrayList<>(cycle.getCompetencies());
                // Sort by display order manually or via stream
                competencies.sort(Comparator.comparingInt(Competency::getDisplayOrder));
            } else {
                competencies = competencyRepository.findByIsActiveTrueOrderByDisplayOrder();
            }

            // Get existing ratings if any
            List<AppraisalRating> existingRatings = ratingRepository.findByReview(review);

            // Fetch goals of the employee for this cycle to help manager review
            Employee reviewee = review.getAppraisal().getEmployee();
            AppraisalCycle cycle = review.getAppraisal().getCycle();
            List<Goal> goals = goalRepository.findByAssignedToAndCycleOrderByDueDateAsc(reviewee, cycle);
            List<Map<String, Object>> goalDTOs = goals.stream().map(g -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", g.getId());
                map.put("title", g.getTitle());
                map.put("weightage", g.getWeightage());
                map.put("progressPct", g.getProgressPct());
                map.put("status", g.getStatus().name());
                map.put("managerComments", g.getManagerComments());
                map.put("actuals", g.getAchievedValue());
                map.put("kpiTarget", g.getTargetMetric());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("review", convertReviewToDTO(review));
            data.put("competencies", competencies);
            data.put("existingRatings", existingRatings);
            data.put("employee", reviewee);
            data.put("goals", goalDTOs);

            // Fetch other submitted reviews for the same appraisal (e.g., self review, peer
            // review)
            List<AppraisalReview> allReviews = reviewRepository.findByAppraisal(review.getAppraisal());
            List<Map<String, Object>> otherReviews = allReviews.stream()
                    .filter(r -> !r.getId().equals(review.getId()))
                    .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("reviewerType", r.getReviewerType().name());
                        map.put("reviewerName", r.getIsAnonymous() ? "Anonymous"
                                : r.getReviewer().getFirstName() + " " + r.getReviewer().getLastName());
                        map.put("overallComments", r.getOverallComments());
                        map.put("strengths", r.getStrengths());
                        map.put("areasOfImprovement", r.getAreasOfImprovement());
                        map.put("overallRating", r.getOverallRating());

                        List<AppraisalRating> rRatings = ratingRepository.findByReview(r);
                        Map<Long, Integer> cRatings = new HashMap<>();
                        Map<Long, String> cComments = new HashMap<>();
                        for (AppraisalRating ar : rRatings) {
                            cRatings.put(ar.getCompetency().getId(), ar.getRating());
                            cComments.put(ar.getCompetency().getId(), ar.getComments());
                        }
                        map.put("competencyRatings", cRatings);
                        map.put("competencyComments", cComments);

                        return map;
                    }).collect(Collectors.toList());

            data.put("otherReviews", otherReviews);

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

            boolean isDraft = false;
            if (request.containsKey("isDraft") && request.get("isDraft") != null) {
                isDraft = Boolean.parseBoolean(request.get("isDraft").toString());
            }

            appraisalService.submitReview(reviewId, overallComments, strengths,
                    areasOfImprovement, ratings, comments, isDraft);

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

    // ==================== RATING / BAND ====================

    /**
     * Preview the goal-based rating breakdown for an appraisal (read-only).
     * GET /api/appraisals/{id}/rating-preview
     *
     * Returns:
     * - goalScore : weighted goal achievement score (0–90+)
     * - competencyScore : normalised competency review score (0–100)
     * - finalScore : 60 % goal + 40 % competency blended score
     * - suggestedRating : auto-calculated band
     * - currentRating : persisted band (if already finalised)
     * - goalBreakdown[] : per-goal contribution details
     * - goalWeight / competencyWeight : split percentages (60 / 40)
     */
    @GetMapping("/{id}/rating-preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> ratingPreview(@PathVariable Long id, Authentication auth) {
        try {
            Appraisal appraisal = appraisalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appraisal not found"));

            Employee currentUser = getEmployeeFromAuth(auth);
            if (!canAccessAppraisal(appraisal, currentUser, auth)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Access denied"));
            }

            Map<String, Object> preview = appraisalService.previewRating(id);
            return ResponseEntity.ok(Map.of("success", true, "preview", preview));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Finalize (save) the performance band for an appraisal.
     * PUT /api/appraisals/{id}/finalize-rating
     *
     * Request body (all optional):
     * { "overrideRating": "EXCEEDS" } ← if absent the auto-calculated band is used
     *
     * Returns the same fields as rating-preview plus the persisted
     * performanceRating.
     */
    @PutMapping("/{id}/finalize-rating")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> finalizeRating(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request,
            Authentication auth) {
        try {
            Appraisal appraisal = appraisalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appraisal not found"));

            Employee currentUser = getEmployeeFromAuth(auth);
            if (!canAccessAppraisal(appraisal, currentUser, auth)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Access denied"));
            }

            String overrideRating = request != null ? request.get("overrideRating") : null;
            Map<String, Object> result = appraisalService.finalizeRating(id, overrideRating);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Performance rating finalized",
                    "result", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Employee acknowledges (agrees/disagrees) with the finalized rating
     * POST /api/appraisals/{id}/acknowledge
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeAppraisal(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            Appraisal appraisal = appraisalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appraisal not found"));

            Employee currentUser = getEmployeeFromAuth(auth);
            if (!appraisal.getEmployee().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("success", false, "error", "Only the employee can acknowledge their appraisal"));
            }

            Boolean agreed = (Boolean) request.get("agreed");
            String comments = (String) request.get("comments");

            appraisal.setEmployeeAgreed(agreed);
            if (agreed != null && !agreed) {
                appraisal.setEmployeeDisagreeComments(comments);
            }
            appraisalRepository.save(appraisal);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message",
                    agreed != null && agreed ? "Appraisal rating accepted."
                            : "Appraisal disagreement submitted for review.",
                    "appraisal", appraisalService.convertToDTO(appraisal)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
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
