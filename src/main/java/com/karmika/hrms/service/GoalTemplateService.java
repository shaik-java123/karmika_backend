package com.karmika.hrms.service;

import com.karmika.hrms.entity.*;
import com.karmika.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the full appraisal goal workflow:
 *
 * 1. Manager creates a GoalTemplate (DRAFT) and adds GoalMetric rows.
 * 2. Manager publishes the template → Goals are individually created
 * for every direct report (isVisibleToEmployee = true).
 * 3. Employees fill in achieved values and submit.
 * 4. Manager reviews submission and approves individual goals.
 * 5. AppraisalService.finalizeRating() uses only approved goals.
 */
@Service
@RequiredArgsConstructor
public class GoalTemplateService {

    private final GoalTemplateRepository templateRepository;
    private final GoalRepository goalRepository;
    private final EmployeeRepository employeeRepository;
    private final AppraisalCycleRepository cycleRepository;

    // ── 1. Template CRUD ──────────────────────────────────────────────

    @Transactional
    public GoalTemplate createTemplate(Long managerId, Long cycleId,
            String name, String description,
            LocalDate submissionDeadline) {
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        AppraisalCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));

        // One template per manager per cycle
        templateRepository.findByManagerAndCycle(manager, cycle).ifPresent(t -> {
            throw new RuntimeException("A template already exists for this cycle. Edit the existing one.");
        });

        GoalTemplate t = new GoalTemplate();
        t.setManager(manager);
        t.setCycle(cycle);
        t.setTemplateName(name);
        t.setDescription(description);
        t.setEmployeeSubmissionDeadline(submissionDeadline);
        t.setStatus(GoalTemplate.TemplateStatus.DRAFT);
        return templateRepository.save(t);
    }

    @Transactional
    public GoalTemplate addOrUpdateMetric(Long templateId, Map<String, Object> body) {
        GoalTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        if (template.getStatus() != GoalTemplate.TemplateStatus.DRAFT) {
            throw new RuntimeException("Cannot edit a published/locked template");
        }

        GoalMetric metric = new GoalMetric();
        metric.setTemplate(template);

        // Pillar
        String pillarStr = (String) body.getOrDefault("pillar", "DELIVERY_EXECUTION");
        metric.setPillar(GoalMetric.MetricPillar.valueOf(pillarStr));

        // Preset or custom
        String presetStr = (String) body.getOrDefault("presetMetric", "CUSTOM");
        GoalMetric.PresetMetric preset = GoalMetric.PresetMetric.valueOf(presetStr);
        metric.setPresetMetric(preset);

        if (preset == GoalMetric.PresetMetric.CUSTOM) {
            String customName = (String) body.get("customMetricName");
            if (customName == null || customName.isBlank()) {
                throw new RuntimeException("customMetricName is required for CUSTOM metrics");
            }
            metric.setCustomMetricName(customName);
            metric.setUnit((String) body.getOrDefault("unit", ""));
        } else {
            metric.setCustomMetricName(null);
            metric.setUnit(preset.defaultUnit);
        }

        metric.setDescription((String) body.getOrDefault("description", ""));
        if (body.get("targetValue") != null) {
            metric.setTargetValue(Double.parseDouble(body.get("targetValue").toString()));
        }
        metric.setWeightage(body.containsKey("weightage")
                ? Integer.parseInt(body.get("weightage").toString())
                : 10);
        metric.setDisplayOrder(template.getMetrics().size());

        template.getMetrics().add(metric);
        return templateRepository.save(template);
    }

    @Transactional
    public GoalTemplate removeMetric(Long templateId, Long metricId) {
        GoalTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        if (template.getStatus() != GoalTemplate.TemplateStatus.DRAFT) {
            throw new RuntimeException("Cannot edit a published/locked template");
        }
        template.getMetrics().removeIf(m -> m.getId().equals(metricId));
        return templateRepository.save(template);
    }

    // ── 2. Bulk import metrics from parsed CSV/Excel rows ─────────────

    @Transactional
    public GoalTemplate bulkAddMetrics(Long templateId, List<Map<String, Object>> rows) {
        GoalTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        if (template.getStatus() != GoalTemplate.TemplateStatus.DRAFT) {
            throw new RuntimeException("Cannot edit a published/locked template");
        }
        for (Map<String, Object> row : rows) {
            GoalMetric metric = new GoalMetric();
            metric.setTemplate(template);
            String pillarStr = (String) row.getOrDefault("pillar", "DELIVERY_EXECUTION");
            metric.setPillar(GoalMetric.MetricPillar.valueOf(pillarStr));
            String presetStr = (String) row.getOrDefault("presetMetric", "CUSTOM");
            GoalMetric.PresetMetric preset = GoalMetric.PresetMetric.valueOf(presetStr);
            metric.setPresetMetric(preset);
            if (preset == GoalMetric.PresetMetric.CUSTOM) {
                metric.setCustomMetricName((String) row.getOrDefault("customMetricName", "Custom"));
            }
            metric.setUnit((String) row.getOrDefault("unit", preset.defaultUnit));
            metric.setDescription((String) row.getOrDefault("description", ""));
            if (row.get("targetValue") != null) {
                metric.setTargetValue(Double.parseDouble(row.get("targetValue").toString()));
            }
            metric.setWeightage(row.containsKey("weightage")
                    ? Integer.parseInt(row.get("weightage").toString())
                    : 10);
            metric.setDisplayOrder(template.getMetrics().size());
            template.getMetrics().add(metric);
        }
        return templateRepository.save(template);
    }

    // ── 3. Publish → fan out Goal rows to all direct reports ─────────

    @Transactional
    public Map<String, Object> publishTemplate(Long templateId) {
        GoalTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        if (template.getStatus() != GoalTemplate.TemplateStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT templates can be published");
        }
        if (template.getMetrics().isEmpty()) {
            throw new RuntimeException("Add at least one metric before publishing");
        }

        // Find all direct reports of the manager
        List<Employee> reports = employeeRepository.findAll().stream()
                .filter(e -> e.getReportingManager() != null
                        && e.getReportingManager().getId().equals(template.getManager().getId())
                        && e.getStatus() == Employee.EmployeeStatus.ACTIVE)
                .collect(Collectors.toList());

        if (reports.isEmpty()) {
            throw new RuntimeException("No active direct reports found for this manager");
        }

        int created = 0;
        for (Employee emp : reports) {
            for (GoalMetric m : template.getMetrics()) {
                // Avoid duplicates
                boolean exists = goalRepository.findAll().stream()
                        .anyMatch(g -> g.getTemplate() != null && g.getTemplate().getId().equals(templateId)
                                && g.getMetric() != null && g.getMetric().getId().equals(m.getId())
                                && g.getAssignedTo().getId().equals(emp.getId()));
                if (exists)
                    continue;

                Goal g = new Goal();
                g.setAssignedTo(emp);
                g.setAssignedBy(template.getManager());
                g.setCycle(template.getCycle());
                g.setTemplate(template);
                g.setMetric(m);
                g.setTitle(m.getPresetMetric() == GoalMetric.PresetMetric.CUSTOM
                        ? m.getCustomMetricName()
                        : m.getPresetMetric().label);
                g.setDescription(m.getDescription());
                g.setPillar(m.getPillar());
                g.setUnit(m.getUnit());
                g.setTargetValue(m.getTargetValue());
                g.setWeightage(m.getWeightage());
                g.setDueDate(template.getEmployeeSubmissionDeadline());
                g.setIsVisibleToEmployee(true);
                g.setStatus(Goal.GoalStatus.NOT_STARTED);
                goalRepository.save(g);
                created++;
            }
        }

        template.setStatus(GoalTemplate.TemplateStatus.PUBLISHED);
        template.setPublishedAt(LocalDateTime.now());
        templateRepository.save(template);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goalsCreated", created);
        result.put("employeesNotified", reports.size());
        result.put("metricsPerEmployee", template.getMetrics().size());
        return result;
    }

    // ── 4. Lock template (close employee submission) ──────────────────

    @Transactional
    public void lockTemplate(Long templateId) {
        GoalTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        if (template.getStatus() != GoalTemplate.TemplateStatus.PUBLISHED) {
            throw new RuntimeException("Only PUBLISHED templates can be locked");
        }
        template.setStatus(GoalTemplate.TemplateStatus.LOCKED);
        template.setLockedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    // ── 5. Manager approves/rejects an employee's goal submission ─────

    @Transactional
    public Goal approveGoal(Long goalId, String managerComments) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        goal.setManagerApproved(true);
        goal.setManagerApprovedAt(LocalDateTime.now());
        if (managerComments != null)
            goal.setManagerComments(managerComments);
        return goalRepository.save(goal);
    }

    @Transactional
    public Goal rejectGoal(Long goalId, String managerComments) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        goal.setManagerApproved(false);
        goal.setEmployeeSubmitted(false); // employee must re-submit
        if (managerComments != null)
            goal.setManagerComments(managerComments);
        return goalRepository.save(goal);
    }

    // ── 6. Employee submits actuals ────────────────────────────────────

    @Transactional
    public Goal submitActuals(Long goalId, Double achievedValue,
            Integer progressPct, String selfComments) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        if (!goal.getIsVisibleToEmployee()) {
            throw new RuntimeException("Goal is not yet visible — manager has not published yet");
        }

        if (achievedValue != null) {
            goal.setAchievedValue(achievedValue);
            if (goal.getTargetValue() != null && goal.getTargetValue() > 0) {
                int auto = (int) Math.min(100, Math.round(achievedValue / goal.getTargetValue() * 100));
                goal.setProgressPct(auto);
            }
        }
        if (progressPct != null) {
            goal.setProgressPct(Math.min(100, Math.max(0, progressPct)));
        }
        if (selfComments != null)
            goal.setSelfComments(selfComments);

        // Auto-status
        if (goal.getProgressPct() >= 100) {
            goal.setStatus(Goal.GoalStatus.COMPLETED);
            goal.setCompletedDate(LocalDate.now());
        } else if (goal.getProgressPct() > 0) {
            goal.setStatus(Goal.GoalStatus.IN_PROGRESS);
        }

        goal.setEmployeeSubmitted(true);
        goal.setEmployeeSubmittedAt(LocalDateTime.now());
        goal.setManagerApproved(false); // reset approval after edit
        return goalRepository.save(goal);
    }

    // ── 7. Queries ────────────────────────────────────────────────────

    public List<GoalTemplate> getMyTemplates(Long managerId) {
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return templateRepository.findByManagerOrderByCreatedAtDesc(manager);
    }

    /** Goals visible to an employee (isVisibleToEmployee = true) */
    public List<Goal> getVisibleGoalsForEmployee(Long employeeId) {
        return goalRepository.findAll().stream()
                .filter(g -> g.getAssignedTo().getId().equals(employeeId)
                        && Boolean.TRUE.equals(g.getIsVisibleToEmployee()))
                .collect(Collectors.toList());
    }

    /** All goals for a manager's template (for the approval view) */
    public List<Goal> getGoalsByTemplate(Long templateId) {
        return goalRepository.findAll().stream()
                .filter(g -> g.getTemplate() != null
                        && g.getTemplate().getId().equals(templateId))
                .collect(Collectors.toList());
    }

    // ── Serialisation helpers ─────────────────────────────────────────

    public Map<String, Object> templateToDTO(GoalTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("templateName", t.getTemplateName());
        m.put("description", t.getDescription());
        m.put("status", t.getStatus().name());
        m.put("cycleName", t.getCycle().getCycleName());
        m.put("cycleId", t.getCycle().getId());
        m.put("cycleType", t.getCycle().getCycleType().name());
        m.put("submissionDeadline", t.getEmployeeSubmissionDeadline() != null
                ? t.getEmployeeSubmissionDeadline().toString()
                : null);
        m.put("publishedAt", t.getPublishedAt() != null ? t.getPublishedAt().toString() : null);
        m.put("lockedAt", t.getLockedAt() != null ? t.getLockedAt().toString() : null);
        m.put("metricCount", t.getMetrics().size());
        m.put("metrics", t.getMetrics().stream().map(this::metricToDTO).collect(Collectors.toList()));
        m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        return m;
    }

    public Map<String, Object> metricToDTO(GoalMetric m) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", m.getId());
        r.put("pillar", m.getPillar().name());
        r.put("presetMetric", m.getPresetMetric().name());
        r.put("label", m.getPresetMetric() == GoalMetric.PresetMetric.CUSTOM
                ? m.getCustomMetricName()
                : m.getPresetMetric().label);
        r.put("customMetricName", m.getCustomMetricName());
        r.put("description", m.getDescription());
        r.put("unit", m.getUnit());
        r.put("targetValue", m.getTargetValue());
        r.put("weightage", m.getWeightage());
        r.put("displayOrder", m.getDisplayOrder());
        return r;
    }

    public Map<String, Object> goalToDTO(Goal g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("title", g.getTitle());
        m.put("description", g.getDescription());
        m.put("pillar", g.getPillar() != null ? g.getPillar().name() : null);
        m.put("unit", g.getUnit());
        m.put("targetValue", g.getTargetValue());
        m.put("achievedValue", g.getAchievedValue());
        m.put("progressPct", g.getProgressPct());
        m.put("weightage", g.getWeightage());
        m.put("status", g.getStatus().name());
        m.put("priority", g.getPriority().name());
        m.put("dueDate", g.getDueDate() != null ? g.getDueDate().toString() : null);
        m.put("completedDate", g.getCompletedDate() != null ? g.getCompletedDate().toString() : null);
        m.put("isVisibleToEmployee", g.getIsVisibleToEmployee());
        m.put("employeeSubmitted", g.getEmployeeSubmitted());
        m.put("employeeSubmittedAt", g.getEmployeeSubmittedAt() != null ? g.getEmployeeSubmittedAt().toString() : null);
        m.put("managerApproved", g.getManagerApproved());
        m.put("managerApprovedAt", g.getManagerApprovedAt() != null ? g.getManagerApprovedAt().toString() : null);
        m.put("managerComments", g.getManagerComments());
        m.put("selfComments", g.getSelfComments());
        m.put("cycleId", g.getCycle() != null ? g.getCycle().getId() : null);
        m.put("cycleName", g.getCycle() != null ? g.getCycle().getCycleName() : null);
        m.put("templateId", g.getTemplate() != null ? g.getTemplate().getId() : null);
        m.put("metricId", g.getMetric() != null ? g.getMetric().getId() : null);
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
        return m;
    }

    /** Catalogue of all preset metrics (for the frontend form dropdowns) */
    public List<Map<String, Object>> getPresetCatalogue() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GoalMetric.PresetMetric pm : GoalMetric.PresetMetric.values()) {
            if (pm == GoalMetric.PresetMetric.CUSTOM)
                continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", pm.name());
            item.put("label", pm.label);
            item.put("pillar", pm.pillar.name());
            item.put("defaultUnit", pm.defaultUnit);
            list.add(item);
        }
        return list;
    }
}
