package com.karmika.hrms.controller;

import com.karmika.hrms.dto.SalarySlipDTO;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.EmployeeSalaryStructure;
import com.karmika.hrms.entity.SalarySlip;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.EmployeeSalaryStructureRepository;
import com.karmika.hrms.repository.SalarySlipRepository;
import com.karmika.hrms.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final EmployeeRepository employeeRepository;
    private final SalarySlipRepository salarySlipRepository;
    private final EmployeeSalaryStructureRepository salaryStructureRepository;
    private final com.karmika.hrms.repository.SalaryComponentRepository salaryComponentRepository;

    /**
     * Set employee salary structure (ADMIN & HR)
     * Accepts a list of flat maps: [{componentId, amount, isActive}]
     */
    @PostMapping("/employee/{employeeId}/salary-structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Transactional
    public ResponseEntity<?> setEmployeeSalaryStructure(
            @PathVariable Long employeeId,
            @RequestBody List<Map<String, Object>> structureRequests) {
        try {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            // Step 1: Hard delete all existing structures for this employee
            List<EmployeeSalaryStructure> existing = salaryStructureRepository.findByEmployee(employee);
            if (!existing.isEmpty()) {
                salaryStructureRepository.deleteAll(existing);
                salaryStructureRepository.flush();
            }

            // Step 2: Save each new structure from the flat request map
            for (Map<String, Object> req : structureRequests) {
                Object compIdObj = req.get("componentId");
                Object amountObj = req.get("amount");
                if (compIdObj == null || amountObj == null)
                    continue;

                Long componentId = Long.valueOf(compIdObj.toString());
                Double amount = Double.valueOf(amountObj.toString());
                Boolean isActive = req.get("isActive") != null
                        ? Boolean.valueOf(req.get("isActive").toString())
                        : true;

                com.karmika.hrms.entity.SalaryComponent component = salaryComponentRepository
                        .findById(componentId)
                        .orElseThrow(() -> new RuntimeException("Salary Component not found: " + componentId));

                EmployeeSalaryStructure structure = new EmployeeSalaryStructure();
                structure.setEmployee(employee);
                structure.setComponent(component);
                structure.setAmount(amount);
                structure.setIsActive(isActive);
                salaryStructureRepository.save(structure);
            }

            // Return a fresh read of the saved structures as flat DTOs
            List<Map<String, Object>> savedDtos = buildStructureDtos(
                    salaryStructureRepository.findByEmployeeAndIsActiveTrue(employee));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Salary structure updated successfully for " + employee.getFirstName(),
                    "structures", savedDtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get employee salary structure (ADMIN & HR)
     * Returns flat DTOs to avoid Jackson serialization issues with deep entity
     * graphs.
     */
    @GetMapping("/employee/{employeeId}/salary-structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> getEmployeeSalaryStructure(@PathVariable Long employeeId) {
        try {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            List<EmployeeSalaryStructure> structures = salaryStructureRepository
                    .findByEmployeeAndIsActiveTrue(employee);

            List<Map<String, Object>> dtos = buildStructureDtos(structures);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "structures", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Helper: convert EmployeeSalaryStructure list to flat, safe maps for JSON
     * response.
     */
    private List<Map<String, Object>> buildStructureDtos(List<EmployeeSalaryStructure> structures) {
        List<Map<String, Object>> dtos = new java.util.ArrayList<>();
        for (EmployeeSalaryStructure s : structures) {
            Map<String, Object> dto = new java.util.LinkedHashMap<>();
            dto.put("id", s.getId());
            dto.put("componentId", s.getComponent().getId());
            dto.put("componentCode", s.getComponent().getCode());
            dto.put("componentName", s.getComponent().getName());
            dto.put("componentType", s.getComponent().getType().name());
            dto.put("calculationType", s.getComponent().getCalculationType().name());
            dto.put("defaultPercentage", s.getComponent().getDefaultPercentage());
            dto.put("amount", s.getAmount());
            dto.put("isActive", s.getIsActive());
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Generate salary slip for single employee (ADMIN & HR)
     */
    @PostMapping("/generate-slip")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> generateSalarySlip(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            Long employeeId = Long.valueOf(request.get("employeeId").toString());
            Integer month = Integer.valueOf(request.get("month").toString());
            Integer year = Integer.valueOf(request.get("year").toString());

            String username = auth.getName();
            Employee generatedBy = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElse(null);

            SalarySlip slip = payrollService.generateSalarySlip(employeeId, month, year, generatedBy);
            SalarySlipDTO dto = payrollService.convertToDTO(slip);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Salary slip generated successfully",
                    "slip", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Generate bulk salary slips for all employees (ADMIN & HR)
     */
    @PostMapping("/generate-bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> generateBulkSalarySlips(
            @RequestBody Map<String, Integer> request,
            Authentication auth) {
        try {
            Integer month = request.get("month");
            Integer year = request.get("year");

            String username = auth.getName();
            Employee generatedBy = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElse(null);

            List<SalarySlip> slips = payrollService.generateBulkSalarySlips(month, year, generatedBy);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Generated " + slips.size() + " salary slips",
                    "count", slips.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get all salary slips for a month/year (ADMIN, HR & FINANCE)
     */
    @GetMapping("/slips")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'FINANCE')")
    public ResponseEntity<?> getSalarySlips(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            List<SalarySlip> slips;

            if (month != null && year != null) {
                slips = salarySlipRepository.findByMonthAndYear(month, year);
            } else {
                slips = salarySlipRepository.findAll();
            }

            List<SalarySlipDTO> dtos = slips.stream()
                    .map(payrollService::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "slips", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get salary slip by ID (ADMIN, HR, or own slip)
     */
    @GetMapping("/slip/{id}")
    public ResponseEntity<?> getSalarySlipById(@PathVariable Long id, Authentication auth) {
        try {
            return salarySlipRepository.findById(id)
                    .map(slip -> {
                        // Check if user can access this slip
                        String username = auth.getName();
                        boolean isAdminOrHR = auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                                        a.getAuthority().equals("ROLE_HR"));

                        boolean isOwnSlip = slip.getEmployee().getUser() != null &&
                                slip.getEmployee().getUser().getUsername().equals(username);

                        if (!isAdminOrHR && !isOwnSlip) {
                            return ResponseEntity.status(403).body(Map.of(
                                    "success", false,
                                    "error", "Access denied"));
                        }

                        SalarySlipDTO dto = payrollService.convertToDTO(slip);
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "slip", dto));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get my salary slips (All authenticated users)
     */
    @GetMapping("/my-slips")
    public ResponseEntity<?> getMySalarySlips(Authentication auth) {
        try {
            String username = auth.getName();
            Employee employee = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            List<SalarySlip> slips = salarySlipRepository.findByEmployee(employee);
            List<SalarySlipDTO> dtos = slips.stream()
                    .map(payrollService::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "slips", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Update payment status (ADMIN & HR)
     */
    @PutMapping("/slip/{id}/payment-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            return salarySlipRepository.findById(id)
                    .map(slip -> {
                        String status = request.get("status");
                        slip.setPaymentStatus(SalarySlip.PaymentStatus.valueOf(status));

                        if (status.equals("PAID") && request.containsKey("paymentDate")) {
                            slip.setPaymentDate(java.time.LocalDate.parse(request.get("paymentDate")));
                        }

                        if (request.containsKey("remarks")) {
                            slip.setRemarks(request.get("remarks"));
                        }

                        salarySlipRepository.save(slip);

                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Payment status updated"));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Regenerate (refresh) a salary slip with latest structure (ADMIN & HR)
     */
    @PostMapping("/slip/{id}/regenerate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Transactional
    public ResponseEntity<?> regenerateSalarySlip(
            @PathVariable Long id,
            Authentication auth) {
        try {
            SalarySlip existingSlip = salarySlipRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Salary slip not found"));

            if (existingSlip.getApprovalStatus() == SalarySlip.ApprovalStatus.APPROVED) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Cannot regenerate an already approved slip"));
            }

            String username = auth.getName();
            Employee generatedBy = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElse(null);

            // Delete old slip, then regenerate
            Long employeeId = existingSlip.getEmployee().getId();
            Integer month = existingSlip.getMonth();
            Integer year = existingSlip.getYear();
            salarySlipRepository.delete(existingSlip);
            salarySlipRepository.flush();

            SalarySlip newSlip = payrollService.generateSalarySlip(employeeId, month, year, generatedBy);
            SalarySlipDTO dto = payrollService.convertToDTO(newSlip);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Salary slip regenerated with latest values",
                    "slip", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Approve salary slip (ADMIN & FINANCE)
     */
    @PutMapping("/slip/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<?> approveSalarySlip(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request,
            Authentication auth) {
        try {
            return salarySlipRepository.findById(id)
                    .map(slip -> {
                        // Check if already approved or rejected
                        if (slip.getApprovalStatus() == SalarySlip.ApprovalStatus.APPROVED) {
                            return ResponseEntity.badRequest().body(Map.of(
                                    "success", false,
                                    "error", "Salary slip is already approved"));
                        }

                        String username = auth.getName();
                        Employee approver = employeeRepository.findAll().stream()
                                .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                                .findFirst()
                                .orElse(null);

                        slip.setApprovalStatus(SalarySlip.ApprovalStatus.APPROVED);
                        slip.setApprovedBy(approver);
                        slip.setApprovedAt(java.time.LocalDateTime.now());

                        if (request != null && request.containsKey("remarks")) {
                            slip.setApprovalRemarks(request.get("remarks"));
                        }

                        // Auto-update payment status to PROCESSED when approved
                        if (slip.getPaymentStatus() == SalarySlip.PaymentStatus.PENDING) {
                            slip.setPaymentStatus(SalarySlip.PaymentStatus.PROCESSED);
                        }

                        salarySlipRepository.save(slip);

                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Salary slip approved successfully"));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Reject salary slip (ADMIN & FINANCE)
     */
    @PutMapping("/slip/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<?> rejectSalarySlip(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Rejection reason is required"));
            }

            return salarySlipRepository.findById(id)
                    .map(slip -> {
                        String username = auth.getName();
                        Employee rejector = employeeRepository.findAll().stream()
                                .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                                .findFirst()
                                .orElse(null);

                        slip.setApprovalStatus(SalarySlip.ApprovalStatus.REJECTED);
                        slip.setRejectedBy(rejector);
                        slip.setRejectedAt(java.time.LocalDateTime.now());
                        slip.setRejectionReason(reason);

                        salarySlipRepository.save(slip);

                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Salary slip rejected"));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get slips pending approval (ADMIN & FINANCE)
     */
    @GetMapping("/slips/pending-approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<?> getPendingApprovalSlips() {
        try {
            List<SalarySlip> slips = salarySlipRepository.findAll().stream()
                    .filter(slip -> slip.getApprovalStatus() == SalarySlip.ApprovalStatus.PENDING_APPROVAL)
                    .collect(Collectors.toList());

            List<SalarySlipDTO> dtos = slips.stream()
                    .map(payrollService::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "slips", dtos,
                    "count", dtos.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Bulk approve salary slips (ADMIN & FINANCE)
     */
    @PostMapping("/slips/bulk-approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<?> bulkApproveSalarySlips(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> slipIds = (List<Long>) request.get("slipIds");
            String remarks = (String) request.get("remarks");

            if (slipIds == null || slipIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "No slip IDs provided"));
            }

            String username = auth.getName();
            Employee approver = employeeRepository.findAll().stream()
                    .filter(emp -> emp.getUser() != null && emp.getUser().getUsername().equals(username))
                    .findFirst()
                    .orElse(null);

            int approvedCount = 0;
            for (Long slipId : slipIds) {
                Optional<SalarySlip> slipOpt = salarySlipRepository.findById(slipId);
                if (slipOpt.isPresent()) {
                    SalarySlip slip = slipOpt.get();
                    if (slip.getApprovalStatus() == SalarySlip.ApprovalStatus.PENDING_APPROVAL) {
                        slip.setApprovalStatus(SalarySlip.ApprovalStatus.APPROVED);
                        slip.setApprovedBy(approver);
                        slip.setApprovedAt(java.time.LocalDateTime.now());
                        slip.setApprovalRemarks(remarks);

                        if (slip.getPaymentStatus() == SalarySlip.PaymentStatus.PENDING) {
                            slip.setPaymentStatus(SalarySlip.PaymentStatus.PROCESSED);
                        }

                        salarySlipRepository.save(slip);
                        approvedCount++;
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Approved " + approvedCount + " salary slips",
                    "count", approvedCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }
}
