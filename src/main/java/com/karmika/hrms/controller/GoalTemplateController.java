package com.karmika.hrms.controller;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Goal;
import com.karmika.hrms.entity.GoalMetric;
import com.karmika.hrms.entity.GoalTemplate;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.service.GoalTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for the revised appraisal goal workflow.
 *
 * Base path: /api/goal-templates
 *
 * Manager workflow:
 * POST / → create template (DRAFT)
 * POST /{id}/metrics → add one metric
 * POST /{id}/metrics/bulk → bulk-add from CSV rows
 * DELETE /{id}/metrics/{metricId} → remove metric
 * POST /{id}/publish → fan-out Goals to reports
 * POST /{id}/lock → close employee submission window
 * GET / → my templates
 * GET /{id}/goals → all Goals for this template
 * PUT /goals/{goalId}/approve → approve employee submission
 * PUT /goals/{goalId}/reject → reject / ask re-submit
 *
 * Employee workflow:
 * GET /my-goals → visible goals
 * PUT /goals/{goalId}/submit → submit actuals
 *
 * Global:
 * GET /preset-catalogue → list of preset metrics
 */
@RestController
@RequestMapping("/api/goal-templates")
@RequiredArgsConstructor
public class GoalTemplateController {

    private final GoalTemplateService templateService;
    private final EmployeeRepository employeeRepository;

    // ── Helper ────────────────────────────────────────────────────────

    private Employee currentEmployee(Authentication auth) {
        String username = auth.getName();
        return employeeRepository.findByEmail(username)
                .or(() -> employeeRepository.findByEmployeeId(username))
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    // ── Manager: Template CRUD ─────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Employee manager = currentEmployee(auth);

            Long cycleId = Long.parseLong(body.get("cycleId").toString());
            String name = (String) body.getOrDefault("templateName", "Goal Template");
            String desc = (String) body.getOrDefault("description", "");
            LocalDate deadline = LocalDate.parse((String) body.get("submissionDeadline"));

            GoalTemplate template = templateService.createTemplate(
                    manager.getId(), cycleId, name, desc, deadline);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "template", templateService.templateToDTO(template)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> getMyTemplates(Authentication auth) {
        try {
            Employee manager = currentEmployee(auth);
            List<Map<String, Object>> dtos = templateService.getMyTemplates(manager.getId())
                    .stream().map(templateService::templateToDTO).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("success", true, "templates", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        try {
            GoalTemplate t = templateService.getMyTemplates(null).stream()
                    .filter(x -> x.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new RuntimeException("Template not found"));
            return ResponseEntity.ok(Map.of("success", true, "template", templateService.templateToDTO(t)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Manager: Metric management ─────────────────────────────────────

    @PostMapping("/{id}/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> addMetric(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            GoalTemplate t = templateService.addOrUpdateMetric(id, body);
            return ResponseEntity.ok(Map.of("success", true, "template", templateService.templateToDTO(t)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/metrics/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> bulkAddMetrics(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("metrics");
            GoalTemplate t = templateService.bulkAddMetrics(id, rows);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "added", rows.size(),
                    "template", templateService.templateToDTO(t)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/metrics/{metricId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> removeMetric(@PathVariable Long id,
            @PathVariable Long metricId) {
        try {
            GoalTemplate t = templateService.removeMetric(id, metricId);
            return ResponseEntity.ok(Map.of("success", true, "template", templateService.templateToDTO(t)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Manager: Lifecycle ─────────────────────────────────────────────

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> publishTemplate(@PathVariable Long id) {
        try {
            Map<String, Object> result = templateService.publishTemplate(id);
            return ResponseEntity.ok(Map.of("success", true, "result", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> lockTemplate(@PathVariable Long id) {
        try {
            templateService.lockTemplate(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Template locked for rating"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Manager: Goal approval view ────────────────────────────────────

    @GetMapping("/{id}/goals")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> getTemplateGoals(@PathVariable Long id) {
        try {
            List<Map<String, Object>> goals = templateService.getGoalsByTemplate(id)
                    .stream().map(templateService::goalToDTO).collect(Collectors.toList());

            // Group by employee for convenient display
            Map<String, Object> byEmployee = new LinkedHashMap<>();
            for (Map<String, Object> g : goals) {
                @SuppressWarnings("unchecked")
                Map<String, Object> emp = (Map<String, Object>) g.get("assignedTo");
                if (emp == null)
                    continue;
                String empKey = emp.get("id").toString();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> empGoals = (List<Map<String, Object>>) byEmployee.computeIfAbsent(empKey,
                        k -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("employee", emp);
                            row.put("goals", new ArrayList<Map<String, Object>>());
                            return row;
                        });
                // This is a bit tricky; restructure
            }

            return ResponseEntity.ok(Map.of("success", true, "goals", goals));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/goals/{goalId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> approveGoal(@PathVariable Long goalId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comments = body != null ? body.get("managerComments") : null;
            Goal g = templateService.approveGoal(goalId, comments);
            return ResponseEntity.ok(Map.of("success", true, "goal", templateService.goalToDTO(g)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/goals/{goalId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<?> rejectGoal(@PathVariable Long goalId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comments = body != null ? body.get("managerComments") : null;
            Goal g = templateService.rejectGoal(goalId, comments);
            return ResponseEntity.ok(Map.of("success", true, "goal", templateService.goalToDTO(g)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Employee: view & submit ────────────────────────────────────────

    @GetMapping("/my-goals")
    public ResponseEntity<?> getMyGoals(Authentication auth) {
        try {
            Employee emp = currentEmployee(auth);
            List<Map<String, Object>> goals = templateService.getVisibleGoalsForEmployee(emp.getId())
                    .stream().map(templateService::goalToDTO).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("success", true, "goals", goals));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/goals/{goalId}/submit")
    public ResponseEntity<?> submitActuals(@PathVariable Long goalId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Double achievedValue = body.get("achievedValue") != null
                    ? Double.parseDouble(body.get("achievedValue").toString())
                    : null;
            Integer progressPct = body.get("progressPct") != null
                    ? Integer.parseInt(body.get("progressPct").toString())
                    : null;
            String selfComments = (String) body.get("selfComments");

            Goal g = templateService.submitActuals(goalId, achievedValue, progressPct, selfComments);
            return ResponseEntity.ok(Map.of("success", true, "goal", templateService.goalToDTO(g)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Global: preset catalogue ───────────────────────────────────────

    @GetMapping("/preset-catalogue")
    public ResponseEntity<?> getPresetCatalogue() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "metrics", templateService.getPresetCatalogue()));
    }
}
