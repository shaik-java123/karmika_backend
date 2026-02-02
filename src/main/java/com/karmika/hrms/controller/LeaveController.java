package com.karmika.hrms.controller;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.LeaveApplication;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.LeaveApplicationRepository;
import com.karmika.hrms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

        private final LeaveApplicationRepository leaveRepository;
        private final EmployeeRepository employeeRepository;
        private final NotificationService notificationService;
        private final com.karmika.hrms.repository.LeaveTypeRepository leaveTypeRepository;

        /**
         * Apply for leave (All authenticated users)
         * POST /api/leave/apply
         */
        @PostMapping("/apply")
        public ResponseEntity<?> applyLeave(@RequestBody Map<String, Object> leaveData, Authentication auth) {
                try {
                        String username = auth.getName();

                        // Find employee by username
                        Employee employee = employeeRepository.findAll().stream()
                                        .filter(emp -> emp.getUser() != null
                                                        && emp.getUser().getUsername().equals(username))
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Employee not found for user: " + username));

                        LeaveApplication leave = new LeaveApplication();
                        leave.setEmployee(employee);

                        String leaveTypeCode = (String) leaveData.get("leaveType");
                        com.karmika.hrms.entity.LeaveType type = leaveTypeRepository.findByCode(leaveTypeCode)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Invalid Leave Type: " + leaveTypeCode));

                        leave.setLeaveType(type);
                        leave.setStartDate(LocalDate.parse((String) leaveData.get("startDate")));
                        leave.setEndDate(LocalDate.parse((String) leaveData.get("endDate")));
                        leave.setReason((String) leaveData.get("reason"));

                        // Calculate number of days
                        long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
                        leave.setNumberOfDays((int) days);
                        leave.setStatus(LeaveApplication.LeaveStatus.PENDING);

                        LeaveApplication savedLeave = leaveRepository.save(leave);

                        // Notify reporting manager
                        if (employee.getReportingManager() != null) {
                                notificationService.notifyLeaveApplication(
                                                employee.getReportingManager(),
                                                employee,
                                                savedLeave.getLeaveType().getName(),
                                                savedLeave.getId());
                        }

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Leave application submitted successfully",
                                        "leave", savedLeave));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Get my leave applications (All authenticated users)
         * GET /api/leave/my-leaves
         */
        @GetMapping("/my-leaves")
        public ResponseEntity<?> getMyLeaves(Authentication auth) {
                try {
                        String username = auth.getName();
                        Employee employee = employeeRepository.findAll().stream()
                                        .filter(emp -> emp.getUser() != null
                                                        && emp.getUser().getUsername().equals(username))
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException("Employee not found"));

                        List<LeaveApplication> leaves = leaveRepository.findAll().stream()
                                        .filter(leave -> leave.getEmployee().getId().equals(employee.getId()))
                                        .toList();

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "leaves", leaves));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Cancel leave application (All authenticated users - own leaves only)
         * DELETE /api/leave/cancel/{id}
         */
        @DeleteMapping("/cancel/{id}")
        public ResponseEntity<?> cancelLeave(@PathVariable Long id, Authentication auth) {
                try {
                        String username = auth.getName();

                        return leaveRepository.findById(id)
                                        .map(leave -> {
                                                // Check if user owns this leave
                                                if (!leave.getEmployee().getUser().getUsername().equals(username)) {
                                                        return ResponseEntity.status(403).body(Map.of(
                                                                        "success", false,
                                                                        "error",
                                                                        "You can only cancel your own leave applications"));
                                                }

                                                // Can only cancel pending leaves
                                                if (leave.getStatus() != LeaveApplication.LeaveStatus.PENDING) {
                                                        return ResponseEntity.badRequest().body(Map.of(
                                                                        "success", false,
                                                                        "error",
                                                                        "Only pending leaves can be cancelled"));
                                                }

                                                leave.setStatus(LeaveApplication.LeaveStatus.CANCELLED);
                                                leaveRepository.save(leave);

                                                return ResponseEntity.ok(Map.of(
                                                                "success", true,
                                                                "message", "Leave cancelled successfully"));
                                        })
                                        .orElse(ResponseEntity.notFound().build());
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Get all leave applications (ADMIN & HR only)
         * GET /api/leave/all
         */
        @GetMapping("/all")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
        public ResponseEntity<List<LeaveApplication>> getAllLeaves() {
                return ResponseEntity.ok(leaveRepository.findAll());
        }

        /**
         * Get pending leave applications (ADMIN, HR, MANAGER)
         * GET /api/leave/pending
         */
        @GetMapping("/pending")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
        public ResponseEntity<List<LeaveApplication>> getPendingLeaves() {
                List<LeaveApplication> pending = leaveRepository.findAll().stream()
                                .filter(leave -> leave.getStatus() == LeaveApplication.LeaveStatus.PENDING)
                                .toList();
                return ResponseEntity.ok(pending);
        }

        /**
         * Get team leave applications (MANAGER - their team only)
         * GET /api/leave/team
         */
        @GetMapping("/team")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
        public ResponseEntity<?> getTeamLeaves(Authentication auth) {
                try {
                        String username = auth.getName();
                        Employee manager = employeeRepository.findAll().stream()
                                        .filter(emp -> emp.getUser() != null
                                                        && emp.getUser().getUsername().equals(username))
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException("Employee not found"));

                        // Get team members (employees reporting to this manager)
                        List<LeaveApplication> teamLeaves = leaveRepository.findAll().stream()
                                        .filter(leave -> leave.getEmployee().getReportingManager() != null &&
                                                        leave.getEmployee().getReportingManager().getId()
                                                                        .equals(manager.getId()))
                                        .toList();

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "leaves", teamLeaves));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Approve leave (ADMIN, HR, MANAGER)
         * POST /api/leave/approve/{id}
         */
        @PostMapping("/approve/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
        public ResponseEntity<?> approveLeave(@PathVariable Long id,
                        @RequestBody(required = false) Map<String, String> data,
                        Authentication auth) {
                try {
                        String username = auth.getName();
                        Employee approver = employeeRepository.findAll().stream()
                                        .filter(emp -> emp.getUser() != null
                                                        && emp.getUser().getUsername().equals(username))
                                        .findFirst()
                                        .orElse(null); // Changed from orElseThrow to orElse(null)

                        return leaveRepository.findById(id)
                                        .map(leave -> {
                                                if (leave.getStatus() != LeaveApplication.LeaveStatus.PENDING) {
                                                        return ResponseEntity.badRequest().body(Map.of(
                                                                        "success", false,
                                                                        "error",
                                                                        "Only pending leaves can be approved"));
                                                }

                                                leave.setStatus(LeaveApplication.LeaveStatus.APPROVED);
                                                leave.setApprovedBy(approver);
                                                leave.setApprovedAt(LocalDateTime.now());

                                                if (data != null && data.containsKey("comments")) {
                                                        leave.setApproverComments(data.get("comments"));
                                                }

                                                leaveRepository.save(leave);

                                                // Send notification to employee
                                                notificationService.notifyLeaveApproval(
                                                                leave.getEmployee(),
                                                                leave.getLeaveType().getName(),
                                                                leave.getId());

                                                return ResponseEntity.ok(Map.of(
                                                                "success", true,
                                                                "message", "Leave approved successfully"));
                                        })
                                        .orElse(ResponseEntity.notFound().build());
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Reject leave (ADMIN, HR, MANAGER)
         * POST /api/leave/reject/{id}
         */
        @PostMapping("/reject/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
        public ResponseEntity<?> rejectLeave(@PathVariable Long id,
                        @RequestBody(required = false) Map<String, String> data,
                        Authentication auth) {
                try {
                        String username = auth.getName();
                        Employee approver = employeeRepository.findAll().stream()
                                        .filter(emp -> emp.getUser() != null
                                                        && emp.getUser().getUsername().equals(username))
                                        .findFirst()
                                        .orElse(null);

                        return leaveRepository.findById(id)
                                        .map(leave -> {
                                                if (leave.getStatus() != LeaveApplication.LeaveStatus.PENDING) {
                                                        return ResponseEntity.badRequest().body(Map.of(
                                                                        "success", false,
                                                                        "error",
                                                                        "Only pending leaves can be rejected"));
                                                }

                                                leave.setStatus(LeaveApplication.LeaveStatus.REJECTED);
                                                leave.setApprovedBy(approver);
                                                leave.setApprovedAt(LocalDateTime.now());

                                                String rejectionReason = null;
                                                if (data != null && data.containsKey("comments")) {
                                                        rejectionReason = data.get("comments");
                                                        leave.setApproverComments(rejectionReason);
                                                }

                                                leaveRepository.save(leave);

                                                // Send notification to employee
                                                notificationService.notifyLeaveRejection(
                                                                leave.getEmployee(),
                                                                leave.getLeaveType().getName(),
                                                                leave.getId(),
                                                                rejectionReason);

                                                return ResponseEntity.ok(Map.of(
                                                                "success", true,
                                                                "message", "Leave rejected successfully"));
                                        })
                                        .orElse(ResponseEntity.notFound().build());
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Get leave statistics
         * GET /api/leave/stats
         */
        @GetMapping("/stats")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
        public ResponseEntity<?> getLeaveStats() {
                List<LeaveApplication> allLeaves = leaveRepository.findAll();

                long pending = allLeaves.stream()
                                .filter(l -> l.getStatus() == LeaveApplication.LeaveStatus.PENDING).count();
                long approved = allLeaves.stream()
                                .filter(l -> l.getStatus() == LeaveApplication.LeaveStatus.APPROVED).count();
                long rejected = allLeaves.stream()
                                .filter(l -> l.getStatus() == LeaveApplication.LeaveStatus.REJECTED).count();

                return ResponseEntity.ok(Map.of(
                                "total", allLeaves.size(),
                                "pending", pending,
                                "approved", approved,
                                "rejected", rejected));
        }
}
