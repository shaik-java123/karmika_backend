package com.karmika.hrms.controller;

import com.karmika.hrms.entity.Department;
import com.karmika.hrms.repository.DepartmentRepository;
import com.karmika.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

        private final DepartmentRepository departmentRepository;
        private final EmployeeRepository employeeRepository;

        /**
         * Create department (ADMIN only)
         * POST /api/departments/create
         */
        @PostMapping("/create")
        @PreAuthorize("hasRole('ADMIN')")
        @org.springframework.cache.annotation.CacheEvict(value = { "departments",
                        "departmentStats" }, allEntries = true)
        public ResponseEntity<?> createDepartment(@RequestBody Map<String, String> data) {
                try {
                        if (departmentRepository.findAll().stream()
                                        .anyMatch(dept -> dept.getName().equalsIgnoreCase(data.get("name")))) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "success", false,
                                                "error", "Department with this name already exists"));
                        }

                        Department department = new Department();
                        department.setName(data.get("name"));
                        department.setDescription(data.get("description"));
                        department.setActive(true);

                        Department saved = departmentRepository.save(department);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Department created successfully",
                                        "department", saved));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Update department (ADMIN only)
         * PUT /api/departments/update/{id}
         */
        @PutMapping("/update/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @org.springframework.cache.annotation.CacheEvict(value = { "departments",
                        "departmentStats" }, allEntries = true)
        public ResponseEntity<?> updateDepartment(@PathVariable Long id, @RequestBody Map<String, String> data) {
                return departmentRepository.findById(id)
                                .map(department -> {
                                        if (data.containsKey("name")) {
                                                department.setName(data.get("name"));
                                        }
                                        if (data.containsKey("description")) {
                                                department.setDescription(data.get("description"));
                                        }

                                        Department updated = departmentRepository.save(department);
                                        return ResponseEntity.ok(Map.of(
                                                        "success", true,
                                                        "message", "Department updated successfully",
                                                        "department", updated));
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * Delete/deactivate department (ADMIN only)
         * DELETE /api/departments/delete/{id}
         */
        @DeleteMapping("/delete/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @org.springframework.cache.annotation.CacheEvict(value = { "departments",
                        "departmentStats" }, allEntries = true)
        public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
                return departmentRepository.findById(id)
                                .map(department -> {
                                        // Check if department has employees
                                        long empCount = employeeRepository.countByDepartmentId(id);

                                        if (empCount > 0) {
                                                // Soft delete - just deactivate
                                                department.setActive(false);
                                                departmentRepository.save(department);
                                                return ResponseEntity.ok(Map.of(
                                                                "success", true,
                                                                "message", "Department deactivated (has " + empCount
                                                                                + " employees)"));
                                        } else {
                                                // Hard delete if no employees
                                                departmentRepository.delete(department);
                                                return ResponseEntity.ok(Map.of(
                                                                "success", true,
                                                                "message", "Department deleted successfully"));
                                        }
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * Get all departments (All authenticated users)
         * GET /api/departments/list
         */
        @GetMapping("/list")
        @org.springframework.cache.annotation.Cacheable("departments")
        public ResponseEntity<List<Department>> getAllDepartments(
                        @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
                List<Department> departments = departmentRepository.findAll();

                if (activeOnly) {
                        departments = departments.stream()
                                        .filter(Department::getActive)
                                        .toList();
                }

                return ResponseEntity.ok(departments);
        }

        /**
         * Get department by ID
         * GET /api/departments/{id}
         */
        @GetMapping("/{id}")
        public ResponseEntity<?> getDepartmentById(@PathVariable Long id) {
                return departmentRepository.findById(id)
                                .map(department -> {
                                        // Count employees in this department
                                        // Count employees in this department
                                        long empCount = employeeRepository.countByDepartmentId(id);

                                        return ResponseEntity.ok(Map.of(
                                                        "success", true,
                                                        "department", department,
                                                        "employeeCount", empCount));
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * Get department statistics
         * GET /api/departments/stats
         */
        @GetMapping("/stats")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
        @org.springframework.cache.annotation.Cacheable("departmentStats")
        public ResponseEntity<?> getDepartmentStats() {
                List<Department> departments = departmentRepository.findAll();

                List<Map<String, Object>> stats = departments.stream()
                                .map(dept -> {
                                        long empCount = employeeRepository.countByDepartmentId(dept.getId());

                                        return Map.<String, Object>of(
                                                        "id", dept.getId(),
                                                        "name", dept.getName(),
                                                        "active", dept.getActive(),
                                                        "employeeCount", empCount);
                                })
                                .toList();

                return ResponseEntity.ok(Map.of(
                                "totalDepartments", departments.size(),
                                "activeDepartments", departments.stream().filter(Department::getActive).count(),
                                "departments", stats));
        }
}
