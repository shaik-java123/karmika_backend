package com.karmika.hrms.controller;

import com.karmika.hrms.entity.*;
import com.karmika.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for Goal / Target management.
 *
 * Endpoints:
 * GET /api/goals/my-goals → goals assigned to me
 * GET /api/goals/team → goals set by me (manager)
 * GET /api/goals/employee/{empId} → goals for a specific employee (manager/HR)
 * GET /api/goals/cycle/{cycleId} → all goals in a cycle (HR/Admin)
 * POST /api/goals → create single goal
 * POST /api/goals/bulk → create multiple goals at once (Excel import)
 * PUT /api/goals/{id}/progress → update progress / achieved value
 * PUT /api/goals/{id}/status → update status
 * PUT /api/goals/{id}/comment → manager comments
 * DELETE /api/goals/{id} → delete goal
 */
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalRepository goalRepository;
    private final EmployeeRepository employeeRepository;
    private final AppraisalCycleRepository cycleRepository;

    // ── GET my goals ──────────────────────────────────────────────────────

    @GetMapping("/my-goals")
    public ResponseEntity<?> getMyGoals(Authentication auth) {
        try {
            Employee me = getEmployee(auth);
            List<Goal> goals = goalRepository.findByAssignedToOrderByDueDateAsc(me);
            return ok("goals", toDTO(goals));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── GET goals I created (team) ────────────────────────────────────────

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> getTeamGoals(Authentication auth) {
        try {
            Employee me = getEmployee(auth);
            List<Goal> goals = goalRepository.findByAssignedByOrderByCreatedAtDesc(me);
            return ok("goals", toDTO(goals));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── GET goals for a specific employee ─────────────────────────────────

    @GetMapping("/employee/{empId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> getGoalsForEmployee(@PathVariable Long empId) {
        try {
            List<Goal> goals = goalRepository.findAllByEmployeeId(empId);
            return ok("goals", toDTO(goals));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── GET goals for a cycle ─────────────────────────────────────────────

    @GetMapping("/cycle/{cycleId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> getGoalsByCycle(@PathVariable Long cycleId) {
        try {
            AppraisalCycle cycle = cycleRepository.findById(cycleId)
                    .orElseThrow(() -> new RuntimeException("Cycle not found"));
            List<Goal> goals = goalRepository.findByCycleOrderByDueDateAsc(cycle);
            return ok("goals", toDTO(goals));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── CREATE single goal ────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> createGoal(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Employee me = getEmployee(auth);
            Goal goal = buildGoal(body, me);
            Goal saved = goalRepository.save(goal);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Goal created successfully",
                    "goal", goalToDTO(saved)));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── BULK create goals (Excel/CSV import) ──────────────────────────────

    /**
     * Accepts a JSON array of goal records (frontend parses the Excel/CSV
     * and sends pre-parsed rows).
     * Body: { goals: [ { employeeId, title, description, targetMetric,
     * targetValue, dueDate, priority, category,
     * weightage, cycleId? }, ... ] }
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> bulkCreateGoals(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Employee me = getEmployee(auth);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("goals");

            if (rows == null || rows.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No goals provided"));
            }

            List<Goal> saved = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (int i = 0; i < rows.size(); i++) {
                try {
                    Goal goal = buildGoal(rows.get(i), me);
                    saved.add(goalRepository.save(goal));
                } catch (Exception ex) {
                    errors.add("Row " + (i + 1) + ": " + ex.getMessage());
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("created", saved.size());
            result.put("failed", errors.size());
            result.put("goals", toDTO(saved));
            if (!errors.isEmpty())
                result.put("errors", errors);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return err(e);
        }
    }

    // ── UPDATE progress ───────────────────────────────────────────────────

    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Goal goal = goalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Goal not found"));

            Employee me = getEmployee(auth);
            checkAccess(goal, me, auth);

            if (body.containsKey("achievedValue")) {
                double achieved = Double.parseDouble(body.get("achievedValue").toString());
                goal.setAchievedValue(achieved);
                // Auto-calculate progress if targetValue is set
                if (goal.getTargetValue() != null && goal.getTargetValue() > 0) {
                    int pct = (int) Math.min(100, Math.round(achieved / goal.getTargetValue() * 100));
                    goal.setProgressPct(pct);
                }
            }
            if (body.containsKey("progressPct")) {
                goal.setProgressPct(Math.min(100, Math.max(0, Integer.parseInt(body.get("progressPct").toString()))));
            }
            if (body.containsKey("selfComments")) {
                goal.setSelfComments((String) body.get("selfComments"));
            }
            // Auto-complete if 100%
            if (goal.getProgressPct() >= 100 && goal.getStatus() == Goal.GoalStatus.IN_PROGRESS) {
                goal.setStatus(Goal.GoalStatus.COMPLETED);
                goal.setCompletedDate(LocalDate.now());
            } else if (goal.getProgressPct() > 0 && goal.getStatus() == Goal.GoalStatus.NOT_STARTED) {
                goal.setStatus(Goal.GoalStatus.IN_PROGRESS);
            }

            goalRepository.save(goal);
            return ResponseEntity.ok(Map.of("success", true, "message", "Progress updated", "goal", goalToDTO(goal)));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── UPDATE status ─────────────────────────────────────────────────────

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            Goal goal = goalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Goal not found"));
            goal.setStatus(Goal.GoalStatus.valueOf(body.get("status")));
            if (goal.getStatus() == Goal.GoalStatus.COMPLETED) {
                goal.setProgressPct(100);
                goal.setCompletedDate(LocalDate.now());
            }
            goalRepository.save(goal);
            return ResponseEntity.ok(Map.of("success", true, "goal", goalToDTO(goal)));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── Manager comment ───────────────────────────────────────────────────

    @PutMapping("/{id}/comment")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            Goal goal = goalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Goal not found"));
            goal.setManagerComments(body.get("managerComments"));
            goalRepository.save(goal);
            return ResponseEntity.ok(Map.of("success", true, "goal", goalToDTO(goal)));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id, Authentication auth) {
        try {
            Goal goal = goalRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Goal not found"));
            Employee me = getEmployee(auth);
            // Only creator or admin/HR can delete
            if (!goal.getAssignedBy().getId().equals(me.getId()) && !isAdminOrHR(auth)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Access denied"));
            }
            goalRepository.delete(goal);
            return ResponseEntity.ok(Map.of("success", true, "message", "Goal deleted"));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Goal buildGoal(Map<String, Object> body, Employee creator) {
        Goal goal = new Goal();

        Long empId = Long.valueOf(body.get("employeeId").toString());
        Employee assignee = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + empId));
        goal.setAssignedTo(assignee);
        goal.setAssignedBy(creator);

        goal.setTitle((String) body.get("title"));
        goal.setDescription((String) body.getOrDefault("description", ""));
        goal.setTargetMetric((String) body.getOrDefault("targetMetric", ""));

        if (body.containsKey("targetValue") && body.get("targetValue") != null) {
            goal.setTargetValue(Double.parseDouble(body.get("targetValue").toString()));
        }
        if (body.containsKey("weightage") && body.get("weightage") != null) {
            goal.setWeightage(Integer.parseInt(body.get("weightage").toString()));
        }
        if (body.containsKey("priority") && body.get("priority") != null) {
            goal.setPriority(Goal.GoalPriority.valueOf(body.get("priority").toString()));
        }
        if (body.containsKey("category") && body.get("category") != null) {
            goal.setCategory(Goal.GoalCategory.valueOf(body.get("category").toString()));
        }

        String dueDateStr = (String) body.get("dueDate");
        goal.setDueDate(LocalDate.parse(dueDateStr));

        if (body.containsKey("cycleId") && body.get("cycleId") != null) {
            Long cycleId = Long.valueOf(body.get("cycleId").toString());
            cycleRepository.findById(cycleId).ifPresent(goal::setCycle);
        }

        return goal;
    }

    private void checkAccess(Goal goal, Employee me, Authentication auth) {
        boolean isOwner = goal.getAssignedTo().getId().equals(me.getId());
        boolean isCreator = goal.getAssignedBy() != null && goal.getAssignedBy().getId().equals(me.getId());
        if (!isOwner && !isCreator && !isAdminOrHR(auth)) {
            throw new RuntimeException("Access denied");
        }
    }

    private Employee getEmployee(Authentication auth) {
        String username = auth.getName();
        return employeeRepository.findAll().stream()
                .filter(e -> e.getUser() != null && e.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    private boolean isAdminOrHR(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
    }

    private ResponseEntity<?> ok(String key, Object data) {
        return ResponseEntity.ok(Map.of("success", true, key, data));
    }

    private ResponseEntity<?> err(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
    }

    private List<Map<String, Object>> toDTO(List<Goal> goals) {
        return goals.stream().map(this::goalToDTO).collect(Collectors.toList());
    }

    private Map<String, Object> goalToDTO(Goal g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("title", g.getTitle());
        m.put("description", g.getDescription());
        m.put("targetMetric", g.getTargetMetric());
        m.put("targetValue", g.getTargetValue());
        m.put("achievedValue", g.getAchievedValue());
        m.put("progressPct", g.getProgressPct());
        m.put("category", g.getCategory() != null ? g.getCategory().name() : null);
        m.put("priority", g.getPriority() != null ? g.getPriority().name() : null);
        m.put("status", g.getStatus() != null ? g.getStatus().name() : null);
        m.put("weightage", g.getWeightage());
        m.put("dueDate", g.getDueDate() != null ? g.getDueDate().toString() : null);
        m.put("completedDate", g.getCompletedDate() != null ? g.getCompletedDate().toString() : null);
        m.put("managerComments", g.getManagerComments());
        m.put("selfComments", g.getSelfComments());
        m.put("cycleId", g.getCycle() != null ? g.getCycle().getId() : null);
        m.put("cycleName", g.getCycle() != null ? g.getCycle().getCycleName() : null);

        if (g.getAssignedTo() != null) {
            m.put("assignedTo", Map.of(
                    "id", g.getAssignedTo().getId(),
                    "name", g.getAssignedTo().getFirstName() + " " + g.getAssignedTo().getLastName(),
                    "designation",
                    g.getAssignedTo().getDesignation() != null ? g.getAssignedTo().getDesignation() : ""));
        }
        if (g.getAssignedBy() != null) {
            m.put("assignedBy", Map.of(
                    "id", g.getAssignedBy().getId(),
                    "name", g.getAssignedBy().getFirstName() + " " + g.getAssignedBy().getLastName()));
        }
        m.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : null);
        return m;
    }
}
