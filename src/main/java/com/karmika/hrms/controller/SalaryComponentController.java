package com.karmika.hrms.controller;

import com.karmika.hrms.entity.SalaryComponent;
import com.karmika.hrms.repository.SalaryComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salary-components")
@RequiredArgsConstructor
public class SalaryComponentController {

    private final SalaryComponentRepository componentRepository;

    /**
     * Get all salary components (ADMIN & HR)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<SalaryComponent>> getAllComponents() {
        return ResponseEntity.ok(componentRepository.findAll());
    }

    /**
     * Get active salary components
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<SalaryComponent>> getActiveComponents() {
        return ResponseEntity.ok(componentRepository.findByIsActiveTrue());
    }

    /**
     * Create salary component (ADMIN & HR)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> createComponent(@RequestBody SalaryComponent component) {
        try {
            // Check if code already exists
            if (componentRepository.findByCode(component.getCode()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Component code already exists"));
            }

            SalaryComponent saved = componentRepository.save(component);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Salary component created successfully",
                    "component", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Update salary component (ADMIN & HR)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> updateComponent(@PathVariable Long id, @RequestBody SalaryComponent component) {
        try {
            return componentRepository.findById(id)
                    .map(existing -> {
                        existing.setName(component.getName());
                        existing.setType(component.getType());
                        existing.setCalculationType(component.getCalculationType());
                        existing.setDefaultPercentage(component.getDefaultPercentage());
                        existing.setIsActive(component.getIsActive());
                        existing.setIsMandatory(component.getIsMandatory());
                        existing.setDescription(component.getDescription());

                        SalaryComponent updated = componentRepository.save(existing);
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Component updated successfully",
                                "component", updated));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Delete salary component (ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteComponent(@PathVariable Long id) {
        try {
            componentRepository.deleteById(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Component deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }
}
