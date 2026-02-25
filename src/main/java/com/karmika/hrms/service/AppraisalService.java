package com.karmika.hrms.service;

import com.karmika.hrms.dto.AppraisalCycleDTO;
import com.karmika.hrms.dto.AppraisalDTO;
import com.karmika.hrms.dto.AppraisalReviewDTO;
import com.karmika.hrms.entity.*;
import com.karmika.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppraisalService {

    private final AppraisalCycleRepository cycleRepository;
    private final AppraisalRepository appraisalRepository;
    private final AppraisalReviewRepository reviewRepository;
    private final AppraisalRatingRepository ratingRepository;
    private final CompetencyRepository competencyRepository;
    private final EmployeeRepository employeeRepository;
    private final GoalRepository goalRepository;

    // ── Rating weights ──────────────────────────────────────────────────────
    /** Fraction of final score contributed by goal achievement (0–100 %). */
    private static final double GOAL_WEIGHT = 0.60;
    /** Fraction of final score contributed by competency review (0–100 %). */
    private static final double COMPETENCY_WEIGHT = 0.40;

    /**
     * Create a new appraisal cycle
     */
    @Transactional
    public AppraisalCycle createCycle(AppraisalCycle cycle, Employee createdBy) {
        cycle.setCreatedBy(createdBy);
        cycle.setStatus(AppraisalCycle.CycleStatus.DRAFT);
        return cycleRepository.save(cycle);
    }

    /**
     * Activate a cycle and create appraisals for all active employees
     */
    @Transactional
    public void activateCycle(Long cycleId) {
        AppraisalCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));

        if (cycle.getStatus() != AppraisalCycle.CycleStatus.DRAFT) {
            throw new RuntimeException("Only draft cycles can be activated");
        }

        // Get all active employees
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE)
                .collect(Collectors.toList());

        // Create appraisals for each employee
        for (Employee employee : activeEmployees) {
            createAppraisalForEmployee(cycle, employee);
        }

        cycle.setStatus(AppraisalCycle.CycleStatus.ACTIVE);
        cycleRepository.save(cycle);
    }

    /**
     * Create appraisal for a single employee in a cycle
     */
    @Transactional
    public Appraisal createAppraisalForEmployee(AppraisalCycle cycle, Employee employee) {
        // Check if appraisal already exists
        Optional<Appraisal> existing = appraisalRepository.findByCycleAndEmployee(cycle, employee);
        if (existing.isPresent()) {
            return existing.get();
        }

        Appraisal appraisal = new Appraisal();
        appraisal.setCycle(cycle);
        appraisal.setEmployee(employee);
        appraisal.setManager(employee.getReportingManager());
        appraisal.setStatus(Appraisal.AppraisalStatus.NOT_STARTED);

        // Set required review counts
        if (cycle.getEnablePeerReview()) {
            appraisal.setPeerReviewsRequired(cycle.getMinPeerReviewers());
        }

        appraisal = appraisalRepository.save(appraisal);

        // Create review records
        createReviewRecords(appraisal, cycle);

        return appraisal;
    }

    /**
     * Create review records for an appraisal
     */
    @Transactional
    public void createReviewRecords(Appraisal appraisal, AppraisalCycle cycle) {
        Employee employee = appraisal.getEmployee();

        // Self Review
        if (cycle.getEnableSelfReview()) {
            createReviewRecord(appraisal, employee, AppraisalReview.ReviewerType.SELF);
        }

        // Manager Review
        if (cycle.getEnableManagerReview() && appraisal.getManager() != null) {
            createReviewRecord(appraisal, appraisal.getManager(), AppraisalReview.ReviewerType.MANAGER);
        }
    }

    /**
     * Create a single review record
     */
    private void createReviewRecord(Appraisal appraisal, Employee reviewer,
            AppraisalReview.ReviewerType reviewerType) {
        AppraisalReview review = new AppraisalReview();
        review.setAppraisal(appraisal);
        review.setReviewer(reviewer);
        review.setReviewerType(reviewerType);
        review.setStatus(AppraisalReview.ReviewStatus.PENDING);
        review.setIsAnonymous(reviewerType == AppraisalReview.ReviewerType.PEER);
        reviewRepository.save(review);
    }

    /**
     * Add peer reviewers to an appraisal
     */
    @Transactional
    public void addPeerReviewers(Long appraisalId, List<Long> peerIds) {
        Appraisal appraisal = appraisalRepository.findById(appraisalId)
                .orElseThrow(() -> new RuntimeException("Appraisal not found"));

        AppraisalCycle cycle = appraisal.getCycle();

        if (!cycle.getEnablePeerReview()) {
            throw new RuntimeException("Peer review not enabled for this cycle");
        }

        if (peerIds.size() < cycle.getMinPeerReviewers()) {
            throw new RuntimeException("Minimum " + cycle.getMinPeerReviewers() + " peer reviewers required");
        }

        if (peerIds.size() > cycle.getMaxPeerReviewers()) {
            throw new RuntimeException("Maximum " + cycle.getMaxPeerReviewers() + " peer reviewers allowed");
        }

        // Remove existing peer reviews
        List<AppraisalReview> existingPeerReviews = reviewRepository
                .findByAppraisalAndReviewerType(appraisal, AppraisalReview.ReviewerType.PEER);
        reviewRepository.deleteAll(existingPeerReviews);

        // Add new peer reviewers
        for (Long peerId : peerIds) {
            Employee peer = employeeRepository.findById(peerId)
                    .orElseThrow(() -> new RuntimeException("Peer employee not found: " + peerId));

            if (peer.getId().equals(appraisal.getEmployee().getId())) {
                throw new RuntimeException("Employee cannot review themselves as peer");
            }

            createReviewRecord(appraisal, peer, AppraisalReview.ReviewerType.PEER);
        }

        appraisal.setPeerReviewsRequired(peerIds.size());
        appraisalRepository.save(appraisal);
    }

    /**
     * Submit a review with ratings
     */
    @Transactional
    public void submitReview(Long reviewId, String overallComments, String strengths,
            String areasOfImprovement, Map<Long, Integer> competencyRatings,
            Map<Long, String> competencyComments, boolean isDraft) {
        AppraisalReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (review.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED) {
            throw new RuntimeException("Review already submitted");
        }

        // Save overall comments
        review.setOverallComments(overallComments);
        review.setStrengths(strengths);
        review.setAreasOfImprovement(areasOfImprovement);

        // Delete existing ratings
        ratingRepository.deleteByReview(review);

        // Save competency ratings
        double totalRating = 0;
        int totalWeight = 0;

        for (Map.Entry<Long, Integer> entry : competencyRatings.entrySet()) {
            Long competencyId = entry.getKey();
            Integer rating = entry.getValue();

            if (rating < 1 || rating > 5) {
                throw new RuntimeException("Rating must be between 1 and 5");
            }

            Competency competency = competencyRepository.findById(competencyId)
                    .orElseThrow(() -> new RuntimeException("Competency not found: " + competencyId));

            AppraisalRating appraisalRating = new AppraisalRating();
            appraisalRating.setReview(review);
            appraisalRating.setCompetency(competency);
            appraisalRating.setRating(rating);
            appraisalRating.setComments(competencyComments.get(competencyId));
            ratingRepository.save(appraisalRating);

            totalRating += rating * competency.getWeightage();
            totalWeight += competency.getWeightage();
        }

        // Calculate overall rating
        double overallRating = totalWeight > 0 ? totalRating / totalWeight : 0;
        review.setOverallRating(overallRating);

        if (isDraft) {
            review.setStatus(AppraisalReview.ReviewStatus.IN_PROGRESS);
        } else {
            review.setStatus(AppraisalReview.ReviewStatus.SUBMITTED);
            review.setSubmittedAt(LocalDateTime.now());
        }

        reviewRepository.save(review);

        // Update appraisal status
        updateAppraisalStatus(review.getAppraisal());
    }

    /**
     * Update appraisal status based on completed reviews
     */
    @Transactional
    public void updateAppraisalStatus(Appraisal appraisal) {
        List<AppraisalReview> reviews = reviewRepository.findByAppraisal(appraisal);

        // Count completed reviews by type
        long selfCompleted = reviews.stream()
                .filter(r -> r.getReviewerType() == AppraisalReview.ReviewerType.SELF)
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .count();

        long managerCompleted = reviews.stream()
                .filter(r -> r.getReviewerType() == AppraisalReview.ReviewerType.MANAGER)
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .count();

        long peerCompleted = reviews.stream()
                .filter(r -> r.getReviewerType() == AppraisalReview.ReviewerType.PEER)
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .count();

        long subordinateCompleted = reviews.stream()
                .filter(r -> r.getReviewerType() == AppraisalReview.ReviewerType.SUBORDINATE)
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .count();

        // Update counts
        appraisal.setSelfReviewCompleted(selfCompleted > 0);
        if (selfCompleted > 0) {
            appraisal.setSelfReviewCompletedAt(LocalDateTime.now());
        }

        appraisal.setManagerReviewCompleted(managerCompleted > 0);
        if (managerCompleted > 0) {
            appraisal.setManagerReviewCompletedAt(LocalDateTime.now());
        }

        appraisal.setPeerReviewsCompleted((int) peerCompleted);
        appraisal.setSubordinateReviewsCompleted((int) subordinateCompleted);

        // Calculate average ratings
        calculateAverageRatings(appraisal, reviews);

        // Update status
        AppraisalCycle cycle = appraisal.getCycle();
        boolean allRequired = true;

        if (cycle.getEnableSelfReview() && !appraisal.getSelfReviewCompleted()) {
            allRequired = false;
        }

        if (cycle.getEnableManagerReview() && !appraisal.getManagerReviewCompleted()) {
            allRequired = false;
        }

        if (cycle.getEnablePeerReview() &&
                appraisal.getPeerReviewsCompleted() < appraisal.getPeerReviewsRequired()) {
            allRequired = false;
        }

        if (allRequired && appraisal.getStatus() != Appraisal.AppraisalStatus.COMPLETED) {
            appraisal.setStatus(Appraisal.AppraisalStatus.COMPLETED);
        } else if (appraisal.getStatus() == Appraisal.AppraisalStatus.NOT_STARTED) {
            appraisal.setStatus(Appraisal.AppraisalStatus.IN_PROGRESS);
        }

        appraisalRepository.save(appraisal);
    }

    /**
     * Calculate average ratings from all reviews
     */
    private void calculateAverageRatings(Appraisal appraisal, List<AppraisalReview> reviews) {
        // Self rating
        reviews.stream()
                .filter(r -> r.getReviewerType() == AppraisalReview.ReviewerType.SELF)
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .findFirst()
                .ifPresent(r -> appraisal.setSelfRating(r.getOverallRating()));

        // Manager rating
        reviews.stream()
                .filter(r -> r.getReviewerType() == AppraisalReview.ReviewerType.MANAGER)
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .findFirst()
                .ifPresent(r -> appraisal.setManagerRating(r.getOverallRating()));

        // Peer average
        double peerAvg = reviews.stream()
                .filter(r -> r.getReviewerType() == AppraisalReview.ReviewerType.PEER)
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .mapToDouble(AppraisalReview::getOverallRating)
                .average()
                .orElse(0.0);
        appraisal.setPeerAverageRating(peerAvg > 0 ? peerAvg : null);

        // Subordinate average
        double subAvg = reviews.stream()
                .filter(r -> r.getReviewerType() == AppraisalReview.ReviewerType.SUBORDINATE)
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .mapToDouble(AppraisalReview::getOverallRating)
                .average()
                .orElse(0.0);
        appraisal.setSubordinateAverageRating(subAvg > 0 ? subAvg : null);

        // Overall rating (weighted average)
        double overall = 0;
        int count = 0;

        if (appraisal.getSelfRating() != null) {
            overall += appraisal.getSelfRating() * 0.2; // 20% weight
            count++;
        }
        if (appraisal.getManagerRating() != null) {
            overall += appraisal.getManagerRating() * 0.5; // 50% weight
            count++;
        }
        if (appraisal.getPeerAverageRating() != null) {
            overall += appraisal.getPeerAverageRating() * 0.2; // 20% weight
            count++;
        }
        if (appraisal.getSubordinateAverageRating() != null) {
            overall += appraisal.getSubordinateAverageRating() * 0.1; // 10% weight
            count++;
        }

        appraisal.setOverallRating(count > 0 ? overall : null);
    }

    /**
     * Approve an appraisal
     */
    @Transactional
    public void approveAppraisal(Long appraisalId, Employee approver, String remarks) {
        Appraisal appraisal = appraisalRepository.findById(appraisalId)
                .orElseThrow(() -> new RuntimeException("Appraisal not found"));

        if (appraisal.getStatus() != Appraisal.AppraisalStatus.COMPLETED) {
            throw new RuntimeException("Only completed appraisals can be approved");
        }

        appraisal.setStatus(Appraisal.AppraisalStatus.APPROVED);
        appraisal.setApprovedBy(approver);
        appraisal.setApprovedAt(LocalDateTime.now());
        appraisal.setApprovalRemarks(remarks);

        appraisalRepository.save(appraisal);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GOAL-BASED RATING FINALIZATION (60 % goals + 40 % competencies)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Compute the goal achievement score (0–100) for an employee in a cycle.
     * Each goal contributes: progressPct × weightage / totalWeightage
     * If no goals exist the score is 0.
     */
    public double computeGoalScore(Long employeeId, Long cycleId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        AppraisalCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));

        List<Goal> goals = goalRepository
                .findByAssignedToAndCycleOrderByDueDateAsc(employee, cycle);

        if (goals.isEmpty())
            return 0.0;

        int totalWeight = goals.stream().mapToInt(Goal::getWeightage).sum();
        double weighted = goals.stream()
                .mapToDouble(g -> (double) g.getProgressPct() * g.getWeightage())
                .sum();

        return totalWeight > 0 ? weighted / totalWeight : 0.0;
    }

    /**
     * Map a 0-100 final score to a PerformanceRating band:
     * ≥ 90 → OUTSTANDING
     * 75-89 → EXCEEDS
     * 60-74 → MEETS
     * 40-59 → NEEDS_IMPROVEMENT
     * < 40 → UNSATISFACTORY
     */
    public Appraisal.PerformanceRating scoreToRating(double score) {
        if (score >= 90)
            return Appraisal.PerformanceRating.OUTSTANDING;
        if (score >= 75)
            return Appraisal.PerformanceRating.EXCEEDS;
        if (score >= 60)
            return Appraisal.PerformanceRating.MEETS;
        if (score >= 40)
            return Appraisal.PerformanceRating.NEEDS_IMPROVEMENT;
        return Appraisal.PerformanceRating.UNSATISFACTORY;
    }

    /**
     * Finalize the performance rating for an appraisal using a 60/40 blend:
     * finalScore = goalScore × 0.60 + normalizedCompetencyScore × 0.40
     *
     * The competency overallRating is on a 1-5 scale; we normalise it to 0-100
     * before blending. If no competency reviews exist, the full weight falls
     * on goal achievement.
     *
     * Optionally, if {@code overrideRating} is not null it is applied directly
     * without recalculating (used when a manager manually overrides the band).
     *
     * @return a map with: goalScore, competencyScore, finalScore, performanceRating
     */
    @Transactional
    public Map<String, Object> finalizeRating(Long appraisalId, String overrideRating) {
        Appraisal appraisal = appraisalRepository.findById(appraisalId)
                .orElseThrow(() -> new RuntimeException("Appraisal not found"));

        double goalScore = computeGoalScore(
                appraisal.getEmployee().getId(),
                appraisal.getCycle().getId());

        // Normalize competency overallRating (1-5 scale) → 0-100
        Double rawReview = appraisal.getOverallRating();
        double competencyScore = rawReview != null ? (rawReview / 5.0) * 100.0 : 0.0;

        // Decide effective weights: if no competency score yet, give 100 % to goals
        double effectiveGoalWeight = rawReview != null ? GOAL_WEIGHT : 1.0;
        double effectiveCompetencyWeight = rawReview != null ? COMPETENCY_WEIGHT : 0.0;

        double finalScore = (goalScore * effectiveGoalWeight)
                + (competencyScore * effectiveCompetencyWeight);

        Appraisal.PerformanceRating band;
        if (overrideRating != null && !overrideRating.isBlank()) {
            band = Appraisal.PerformanceRating.valueOf(overrideRating.trim().toUpperCase());
        } else {
            band = scoreToRating(finalScore);
        }

        appraisal.setPerformanceRating(band);

        // If the employee had previously disagreed, this re-finalization acts as the
        // authoritative override.
        // We set employeeAgreed = true so the employee can no longer disagree again.
        if (appraisal.getEmployeeAgreed() != null && !appraisal.getEmployeeAgreed()) {
            appraisal.setEmployeeAgreed(true);
        }

        appraisalRepository.save(appraisal);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goalScore", Math.round(goalScore * 10.0) / 10.0);
        result.put("competencyScore", Math.round(competencyScore * 10.0) / 10.0);
        result.put("finalScore", Math.round(finalScore * 10.0) / 10.0);
        result.put("performanceRating", band.name());
        result.put("appraisalId", appraisalId);
        return result;
    }

    /**
     * Preview the rating breakdown for an appraisal WITHOUT saving.
     * Returns the same map as finalizeRating but does not persist.
     */
    public Map<String, Object> previewRating(Long appraisalId) {
        Appraisal appraisal = appraisalRepository.findById(appraisalId)
                .orElseThrow(() -> new RuntimeException("Appraisal not found"));

        double goalScore = computeGoalScore(
                appraisal.getEmployee().getId(),
                appraisal.getCycle().getId());
        Double rawReview = appraisal.getOverallRating();
        double competencyScore = rawReview != null ? (rawReview / 5.0) * 100.0 : 0.0;

        double effectiveGoalWeight = rawReview != null ? GOAL_WEIGHT : 1.0;
        double effectiveCompetencyWeight = rawReview != null ? COMPETENCY_WEIGHT : 0.0;

        double finalScore = (goalScore * effectiveGoalWeight)
                + (competencyScore * effectiveCompetencyWeight);

        // Also attach per-goal breakdown for the UI
        Employee employee = appraisal.getEmployee();
        AppraisalCycle cycle = appraisal.getCycle();
        List<Goal> goals = goalRepository
                .findByAssignedToAndCycleOrderByDueDateAsc(employee, cycle);

        List<Map<String, Object>> goalBreakdown = goals.stream().map(g -> {
            Map<String, Object> gm = new LinkedHashMap<>();
            gm.put("id", g.getId());
            gm.put("title", g.getTitle());
            gm.put("weightage", g.getWeightage());
            gm.put("progressPct", g.getProgressPct());
            gm.put("status", g.getStatus().name());
            gm.put("contribution", Math.round(g.getProgressPct() * g.getWeightage() / 100.0 * 10.0) / 10.0);
            return gm;
        }).collect(Collectors.toList());

        // Add review comments summary
        List<AppraisalReview> reviews = reviewRepository.findByAppraisal(appraisal);
        List<Map<String, Object>> reviewComments = reviews.stream()
                .filter(r -> r.getStatus() == AppraisalReview.ReviewStatus.SUBMITTED)
                .map(r -> {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("id", r.getId());
                    rm.put("reviewerType", r.getReviewerType().name());
                    rm.put("reviewerName", r.getIsAnonymous() ? "Anonymous Peer"
                            : r.getReviewer().getFirstName() + " " + r.getReviewer().getLastName());
                    rm.put("strengths", r.getStrengths());
                    rm.put("areasOfImprovement", r.getAreasOfImprovement());
                    rm.put("overallComments", r.getOverallComments());
                    rm.put("rating", r.getOverallRating());
                    return rm;
                }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goalScore", Math.round(goalScore * 10.0) / 10.0);
        result.put("competencyScore", Math.round(competencyScore * 10.0) / 10.0);
        result.put("finalScore", Math.round(finalScore * 10.0) / 10.0);
        result.put("suggestedRating", scoreToRating(finalScore).name());
        result.put("currentRating", appraisal.getPerformanceRating() != null
                ? appraisal.getPerformanceRating().name()
                : null);
        result.put("goalCount", goals.size());
        result.put("goalBreakdown", goalBreakdown);
        result.put("reviewComments", reviewComments);
        result.put("hasCompetencyData", rawReview != null);
        result.put("goalWeight", (int) (GOAL_WEIGHT * 100));
        result.put("competencyWeight", (int) (COMPETENCY_WEIGHT * 100));
        return result;
    }

    /**
     * Convert Appraisal to DTO
     */
    public AppraisalDTO convertToDTO(Appraisal appraisal) {
        Employee employee = appraisal.getEmployee();

        return AppraisalDTO.builder()
                .id(appraisal.getId())
                .cycleId(appraisal.getCycle() != null ? appraisal.getCycle().getId() : null)
                .cycleName(appraisal.getCycle() != null ? appraisal.getCycle().getCycleName() : "Unknown Cycle")
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .designation(employee.getDesignation())
                .department(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .status(appraisal.getStatus().toString())
                .selfReviewCompleted(appraisal.getSelfReviewCompleted())
                .selfReviewCompletedAt(
                        appraisal.getSelfReviewCompletedAt() != null ? appraisal.getSelfReviewCompletedAt().toString()
                                : null)
                .managerReviewCompleted(appraisal.getManagerReviewCompleted())
                .managerReviewCompletedAt(appraisal.getManagerReviewCompletedAt() != null
                        ? appraisal.getManagerReviewCompletedAt().toString()
                        : null)
                .peerReviewsCompleted(appraisal.getPeerReviewsCompleted())
                .peerReviewsRequired(appraisal.getPeerReviewsRequired())
                .subordinateReviewsCompleted(appraisal.getSubordinateReviewsCompleted())
                .subordinateReviewsRequired(appraisal.getSubordinateReviewsRequired())
                .managerId(appraisal.getManager() != null ? appraisal.getManager().getId() : null)
                .managerName(appraisal.getManager() != null
                        ? appraisal.getManager().getFirstName() + " " + appraisal.getManager().getLastName()
                        : null)
                .selfRating(appraisal.getSelfRating())
                .managerRating(appraisal.getManagerRating())
                .peerAverageRating(appraisal.getPeerAverageRating())
                .subordinateAverageRating(appraisal.getSubordinateAverageRating())
                .overallRating(appraisal.getOverallRating())
                .managerComments(appraisal.getManagerComments())
                .employeeComments(appraisal.getEmployeeComments())
                .strengths(appraisal.getStrengths())
                .areasOfImprovement(appraisal.getAreasOfImprovement())
                .goals(appraisal.getGoals())
                .performanceRating(
                        appraisal.getPerformanceRating() != null ? appraisal.getPerformanceRating().toString() : null)
                .employeeAgreed(appraisal.getEmployeeAgreed())
                .employeeDisagreeComments(appraisal.getEmployeeDisagreeComments())
                .approvedBy(appraisal.getApprovedBy() != null
                        ? appraisal.getApprovedBy().getFirstName() + " " + appraisal.getApprovedBy().getLastName()
                        : null)
                .approvedAt(appraisal.getApprovedAt() != null ? appraisal.getApprovedAt().toString() : null)
                .approvalRemarks(appraisal.getApprovalRemarks())
                .build();
    }
}
