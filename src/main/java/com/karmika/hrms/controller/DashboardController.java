package com.karmika.hrms.controller;

import com.karmika.hrms.entity.Attendance;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.LeaveApplication;
import com.karmika.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

        private final EmployeeRepository employeeRepository;
        private final LeaveApplicationRepository leaveRepository;
        private final AttendanceRepository attendanceRepository;
        private final DepartmentRepository departmentRepository;
        private final UserRepository userRepository;

        /**
         * Get dashboard stats for current user
         * GET /api/dashboard/stats
         */
        @GetMapping("/stats")
        public ResponseEntity<?> getDashboardStats(Authentication auth) {
                try {
                        String username = auth.getName();
                        String role = auth.getAuthorities().iterator().next().getAuthority();

                        // Get employee if exists
                        Employee employee = employeeRepository.findAll().stream()
                                        .filter(emp -> emp.getUser() != null
                                                        && emp.getUser().getUsername().equals(username))
                                        .findFirst()
                                        .orElse(null);

                        Map<String, Object> stats = new HashMap<>();
                        stats.put("role", role);
                        stats.put("username", username);

                        if (role.equals("ROLE_ADMIN") || role.equals("ROLE_HR")) {
                                // Admin/HR stats
                                long totalEmployees = employeeRepository.count();
                                long activeEmployees = employeeRepository.findAll().stream()
                                                .filter(emp -> emp.getStatus() == Employee.EmployeeStatus.ACTIVE)
                                                .count();
                                long todayAttendance = attendanceRepository.findAll().stream()
                                                .filter(att -> att.getDate().equals(LocalDate.now()))
                                                .count();

                                stats.put("totalEmployees", totalEmployees);
                                stats.put("activeEmployees", activeEmployees);
                                stats.put("totalDepartments", departmentRepository.count());
                                stats.put("pendingLeaves", leaveRepository.findAll().stream()
                                                .filter(leave -> leave
                                                                .getStatus() == LeaveApplication.LeaveStatus.PENDING)
                                                .count());
                                stats.put("todayAttendance", todayAttendance);

                                // Analytics
                                double attendanceRate = totalEmployees > 0
                                                ? (double) todayAttendance / totalEmployees * 100
                                                : 0;
                                stats.put("attendanceRate", String.format("%.1f%%", attendanceRate));

                                long weekLeaves = leaveRepository.findAll().stream()
                                                .filter(l -> l.getStatus() == LeaveApplication.LeaveStatus.APPROVED)
                                                .filter(l -> l.getStartDate().isAfter(LocalDate.now().minusDays(7)))
                                                .count();
                                stats.put("approvedLeavesThisWeek", weekLeaves);

                                // Recent Activity Feed (Unified)
                                List<Map<String, Object>> recentActivity = new ArrayList<>();

                                // Global Recent Leaves
                                leaveRepository.findAll().stream()
                                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                                .limit(3)
                                                .forEach(l -> {
                                                        Map<String, Object> act = new HashMap<>();
                                                        act.put("title", "Leave: " + l.getEmployee().getFirstName()
                                                                        + " "
                                                                        + (l.getEmployee().getLastName() != null
                                                                                        ? l.getEmployee().getLastName()
                                                                                        : ""));
                                                        act.put("description", l.getLeaveType().getName() + " ("
                                                                        + l.getStatus() + ")");
                                                        act.put("time", l.getCreatedAt().toString());
                                                        act.put("type", "leave");
                                                        act.put("icon", "📋");
                                                        recentActivity.add(act);
                                                });

                                // Recent Attendance
                                attendanceRepository.findAll().stream()
                                                .filter(att -> att.getDate().equals(LocalDate.now()))
                                                .sorted((a, b) -> b.getCheckInTime().compareTo(a.getCheckInTime()))
                                                .limit(3)
                                                .forEach(a -> {
                                                        Map<String, Object> act = new HashMap<>();
                                                        act.put("title", "Check-in: " + a.getEmployee().getFirstName());
                                                        act.put("description", "At " + a.getCheckInTime().toString());
                                                        act.put("time", "Today");
                                                        act.put("type", "attendance");
                                                        act.put("icon", "✅");
                                                        recentActivity.add(act);
                                                });

                                stats.put("recentActivity", recentActivity);

                        } else if (role.equals("ROLE_MANAGER")) {
                                // Manager stats
                                if (employee != null) {
                                        long teamSize = employeeRepository.findAll().stream()
                                                        .filter(emp -> emp.getReportingManager() != null &&
                                                                        emp.getReportingManager().getId()
                                                                                        .equals(employee.getId()))
                                                        .count();
                                        stats.put("teamSize", teamSize);

                                        long pendingTeamLeaves = leaveRepository.findAll().stream()
                                                        .filter(leave -> leave
                                                                        .getStatus() == LeaveApplication.LeaveStatus.PENDING)
                                                        .filter(leave -> leave.getEmployee()
                                                                        .getReportingManager() != null &&
                                                                        leave.getEmployee().getReportingManager()
                                                                                        .getId()
                                                                                        .equals(employee.getId()))
                                                        .count();
                                        stats.put("pendingTeamLeaves", pendingTeamLeaves);
                                }
                                // Add employee stats
                                addEmployeeStats(stats, employee);

                        } else {
                                // Employee stats
                                addEmployeeStats(stats, employee);
                        }

                        return ResponseEntity.ok(stats);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        private void addEmployeeStats(Map<String, Object> stats, Employee employee) {
                if (employee != null) {
                        // My leave balance
                        long leavesTaken = leaveRepository.findAll().stream()
                                        .filter(leave -> leave.getEmployee().getId().equals(employee.getId()))
                                        .filter(leave -> leave.getStatus() == LeaveApplication.LeaveStatus.APPROVED)
                                        .mapToInt(LeaveApplication::getNumberOfDays)
                                        .sum();
                        stats.put("leavesTaken", leavesTaken);

                        long pendingLeaves = leaveRepository.findAll().stream()
                                        .filter(leave -> leave.getEmployee().getId().equals(employee.getId()))
                                        .filter(leave -> leave.getStatus() == LeaveApplication.LeaveStatus.PENDING)
                                        .count();
                        stats.put("myPendingLeaves", pendingLeaves);

                        // Today's attendance
                        Attendance todayAtt = attendanceRepository.findAll().stream()
                                        .filter(att -> att.getEmployee().getId().equals(employee.getId()))
                                        .filter(att -> att.getDate().equals(LocalDate.now()))
                                        .findFirst()
                                        .orElse(null);

                        if (todayAtt != null) {
                                stats.put("todayCheckIn",
                                                todayAtt.getCheckInTime() != null ? todayAtt.getCheckInTime().toString()
                                                                : null);
                                stats.put("todayCheckOut",
                                                todayAtt.getCheckOutTime() != null
                                                                ? todayAtt.getCheckOutTime().toString()
                                                                : null);
                                stats.put("todayWorking Minutes", todayAtt.getWorkingMinutes());
                        } else {
                                stats.put("todayCheckIn", null);
                                stats.put("checkedIn", false);
                        }

                        // Recent Activity for Employee
                        List<Map<String, Object>> recentActivity = new ArrayList<>();

                        // Personal Leaves
                        leaveRepository.findAll().stream()
                                        .filter(l -> l.getEmployee().getId().equals(employee.getId()))
                                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                        .limit(2)
                                        .forEach(l -> {
                                                Map<String, Object> act = new HashMap<>();
                                                act.put("title", "My Leave " + l.getStatus());
                                                act.put("description", l.getLeaveType().getName() + " from "
                                                                + l.getStartDate());
                                                act.put("time", l.getCreatedAt().toString());
                                                act.put("type", "leave");
                                                act.put("icon", "📋");
                                                recentActivity.add(act);
                                        });

                        // Personal Attendance
                        attendanceRepository.findAll().stream()
                                        .filter(att -> att.getEmployee().getId().equals(employee.getId()))
                                        .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                                        .limit(2)
                                        .forEach(a -> {
                                                Map<String, Object> act = new HashMap<>();
                                                act.put("title", "Attendance: " + a.getDate());
                                                act.put("description", "Check-in at " + a.getCheckInTime());
                                                act.put("time", a.getDate().toString());
                                                act.put("type", "attendance");
                                                act.put("icon", "✅");
                                                recentActivity.add(act);
                                        });

                        stats.put("recentActivity", recentActivity);
                }
        }

        /**
         * Get HR dashboard analytics
         * GET /api/dashboard/hr-analytics
         */
        @GetMapping("/hr-analytics")
        public ResponseEntity<?> getHRAnalytics() {
                try {
                        Map<String, Object> analytics = new HashMap<>();

                        // Employee distribution by department
                        Map<String, Long> employeesByDept = employeeRepository.findAll().stream()
                                        .filter(emp -> emp.getDepartment() != null)
                                        .collect(Collectors.groupingBy(
                                                        emp -> emp.getDepartment().getName(),
                                                        Collectors.counting()));
                        analytics.put("employeesByDepartment", employeesByDept);

                        // Leave statistics by type
                        Map<String, Long> leavesByType = leaveRepository.findAll().stream()
                                        .filter(leave -> leave.getStatus() == LeaveApplication.LeaveStatus.APPROVED)
                                        .collect(Collectors.groupingBy(
                                                        leave -> leave.getLeaveType().getName(),
                                                        Collectors.counting()));
                        analytics.put("leavesByType", leavesByType);

                        // Attendance statistics for current month
                        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
                        LocalDate lastDayOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

                        long monthlyAttendance = attendanceRepository.findAll().stream()
                                        .filter(att -> !att.getDate().isBefore(firstDayOfMonth) &&
                                                        !att.getDate().isAfter(lastDayOfMonth))
                                        .count();
                        analytics.put("monthlyAttendanceRecords", monthlyAttendance);

                        // Recent hires (last 30 days)
                        LocalDate days30Ago = LocalDate.now().minusDays(30);
                        long recentHires = employeeRepository.findAll().stream()
                                        .filter(emp -> emp.getJoiningDate() != null &&
                                                        !emp.getJoiningDate().isBefore(days30Ago))
                                        .count();
                        analytics.put("recentHires30Days", recentHires);

                        return ResponseEntity.ok(analytics);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Get quick summary
         * GET /api/dashboard/summary
         */
        @GetMapping("/summary")
        public ResponseEntity<?> getQuickSummary(Authentication auth) {
                try {
                        String role = auth.getAuthorities().iterator().next().getAuthority();

                        Map<String, Object> summary = new HashMap<>();
                        summary.put("role", role);
                        summary.put("timestamp", LocalDate.now().toString());

                        if (role.equals("ROLE_ADMIN") || role.equals("ROLE_HR")) {
                                summary.put("employees", employeeRepository.count());
                                summary.put("departments", departmentRepository.count());
                                summary.put("pendingLeaves", leaveRepository.findAll().stream()
                                                .filter(l -> l.getStatus() == LeaveApplication.LeaveStatus.PENDING)
                                                .count());
                                summary.put("todayPresent", attendanceRepository.findAll().stream()
                                                .filter(att -> att.getDate().equals(LocalDate.now()))
                                                .filter(att -> att.getStatus() == Attendance.AttendanceStatus.PRESENT)
                                                .count());
                        }

                        return ResponseEntity.ok(summary);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }
}
