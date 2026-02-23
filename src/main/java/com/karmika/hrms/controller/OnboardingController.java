package com.karmika.hrms.controller;

import com.karmika.hrms.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    // ──────────────────────────────────────────────────────────
    // HR / ADMIN — Start onboarding & manage checklist
    // ──────────────────────────────────────────────────────────

    /**
     * POST /api/onboarding/employee/{id}/start
     * HR starts onboarding for an employee — seeds default checklist.
     */
    @PostMapping("/employee/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> startOnboarding(@PathVariable Long id, Authentication auth) {
        try {
            var checklist = onboardingService.startOnboarding(id, auth.getName());
            return ResponseEntity.ok(Map.of("success", true, "checklist", checklist));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/onboarding/employee/{id}/checklist
     * HR adds a custom checklist task.
     * Body: { title, description, taskType, dueDate, hrNotes }
     */
    @PostMapping("/employee/{id}/checklist")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> addChecklistTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            var task = onboardingService.addChecklistTask(id, auth.getName(), body);
            return ResponseEntity.ok(Map.of("success", true, "task", task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * PUT /api/onboarding/checklist/{taskId}/review
     * HR approves / rejects / waives a submitted checklist item.
     * Body: { action: "approve"|"reject"|"waive", reason: "..." }
     */
    @PutMapping("/checklist/{taskId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> reviewChecklistTask(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            var task = onboardingService.reviewChecklistTask(
                    taskId, auth.getName(),
                    body.get("action"), body.get("reason"));
            return ResponseEntity.ok(Map.of("success", true, "task", task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * DELETE /api/onboarding/checklist/{taskId}
     * HR removes a checklist item.
     */
    @DeleteMapping("/checklist/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> deleteChecklistTask(@PathVariable Long taskId) {
        try {
            onboardingService.deleteChecklistTask(taskId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Checklist item deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/onboarding/employee/{id}/documents
     * HR uploads a document for an employee (offer letter, NDA, policy, etc.)
     * Multipart: file + documentName + description + documentType
     */
    @PostMapping("/employee/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> uploadHrDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentName") String documentName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "documentType", required = false) String documentType,
            Authentication auth) {
        try {
            var doc = onboardingService.uploadHrDocument(id, auth.getName(),
                    documentName, description, documentType, file);
            return ResponseEntity.ok(Map.of("success", true, "document", doc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────
    // EMPLOYEE — Submit checklist tasks
    // ──────────────────────────────────────────────────────────

    /**
     * POST /api/onboarding/checklist/{taskId}/submit
     * Employee submits a checklist item (with optional file upload).
     * Multipart: file (optional) + notes (optional)
     */
    @PostMapping("/checklist/{taskId}/submit")
    public ResponseEntity<?> submitChecklistTask(
            @PathVariable Long taskId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            Authentication auth) {
        try {
            var task = onboardingService.submitChecklistTask(taskId, auth.getName(), notes, file);
            return ResponseEntity.ok(Map.of("success", true, "task", task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────
    // SHARED — Fetch data
    // ──────────────────────────────────────────────────────────

    /**
     * GET /api/onboarding/employee/{id}
     * Get full onboarding checklist + documents + summary for an employee.
     */
    @GetMapping("/employee/{id}")
    public ResponseEntity<?> getOnboardingData(@PathVariable Long id) {
        try {
            var checklist = onboardingService.getOnboardingStatus(id);
            var documents = onboardingService.getDocuments(id);
            var summary = onboardingService.getOnboardingSummary(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "checklist", checklist,
                    "documents", documents,
                    "summary", summary));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * GET /api/onboarding/employee/{id}/summary
     * Quick progress summary (used for Dashboard badge).
     */
    @GetMapping("/employee/{id}/summary")
    public ResponseEntity<?> getOnboardingSummary(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(onboardingService.getOnboardingSummary(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
