package com.karmika.hrms.controller;

import com.karmika.hrms.dto.ApiResponse;
import com.karmika.hrms.entity.Attendance;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.repository.AttendanceRepository;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.service.AttendanceService;
import com.karmika.hrms.dto.DailyAttendanceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

        private final AttendanceRepository attendanceRepository;
        private final EmployeeRepository employeeRepository;
        private final AttendanceService attendanceService;

        /**
         * Check-in (All authenticated users)
         * POST /api/attendance/checkin
         */
        @PostMapping("/checkin")
        public ResponseEntity<?> checkIn(@RequestBody(required = false) Map<String, Object> data,
                        Authentication auth) {
                try {
                        String username = auth.getName();
                        Employee employee = employeeRepository.findByUserUsername(username)
                                        .orElseThrow(() -> new RuntimeException("Employee not found"));

                        // Check if already checked in today
                        LocalDate today = LocalDate.now();
                        boolean alreadyCheckedIn = attendanceRepository.findByEmployeeAndDate(employee, today)
                                        .isPresent();

                        if (alreadyCheckedIn) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "success", false,
                                                "error", "You have already checked in today"));
                        }

                        Attendance attendance = new Attendance();
                        attendance.setEmployee(employee);
                        attendance.setDate(today);
                        attendance.setCheckInTime(LocalTime.now());
                        attendance.setStatus(Attendance.AttendanceStatus.PRESENT);

                        // Set location if provided
                        if (data != null) {
                                if (data.containsKey("location")) {
                                        attendance.setCheckInLocation((String) data.get("location"));
                                }
                                if (data.containsKey("latitude")) {
                                        attendance.setCheckInLatitude(Double.valueOf(data.get("latitude").toString()));
                                }
                                if (data.containsKey("longitude")) {
                                        attendance.setCheckInLongitude(
                                                        Double.valueOf(data.get("longitude").toString()));
                                }
                        }

                        Attendance saved = attendanceRepository.save(attendance);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Checked in successfully",
                                        "checkInTime", saved.getCheckInTime().toString()));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Check-out (All authenticated users)
         * POST /api/attendance/checkout
         */
        @PostMapping("/checkout")
        public ResponseEntity<?> checkOut(@RequestBody(required = false) Map<String, Object> data,
                        Authentication auth) {
                try {
                        String username = auth.getName();
                        Employee employee = employeeRepository.findByUserUsername(username)
                                        .orElseThrow(() -> new RuntimeException("Employee not found"));

                        // Find today's attendance record
                        LocalDate today = LocalDate.now();
                        Attendance attendance = attendanceRepository.findByEmployeeAndDate(employee, today)
                                        .orElseThrow(() -> new RuntimeException("No check-in record found for today"));

                        if (attendance.getCheckOutTime() != null) {
                                return ResponseEntity.badRequest().body(Map.of(
                                                "success", false,
                                                "error", "You have already checked out today"));
                        }

                        attendance.setCheckOutTime(LocalTime.now());

                        // Calculate working minutes
                        long minutes = ChronoUnit.MINUTES.between(attendance.getCheckInTime(),
                                        attendance.getCheckOutTime());
                        attendance.setWorkingMinutes((int) minutes);

                        // Set checkout location if provided
                        if (data != null) {
                                if (data.containsKey("location")) {
                                        attendance.setCheckOutLocation((String) data.get("location"));
                                }
                                if (data.containsKey("latitude")) {
                                        attendance.setCheckOutLatitude(Double.valueOf(data.get("latitude").toString()));
                                }
                                if (data.containsKey("longitude")) {
                                        attendance.setCheckOutLongitude(
                                                        Double.valueOf(data.get("longitude").toString()));
                                }
                        }

                        Attendance saved = attendanceRepository.save(attendance);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Checked out successfully",
                                        "checkOutTime", saved.getCheckOutTime().toString(),
                                        "workingHours", String.format("%.2f hours", minutes / 60.0)));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Get my attendance records (All authenticated users)
         * GET /api/attendance/my-attendance
         */
        @GetMapping("/my-attendance")
        public ResponseEntity<?> getMyAttendance(@RequestParam(required = false) String from,
                        @RequestParam(required = false) String to,
                        Authentication auth) {
                try {
                        String username = auth.getName();
                        Employee employee = employeeRepository.findByUserUsername(username)
                                        .orElseThrow(() -> new RuntimeException("Employee not found"));

                        List<Attendance> records;
                        if (from != null && to != null) {
                                records = attendanceRepository.findByEmployeeAndDateBetween(
                                                employee, LocalDate.parse(from), LocalDate.parse(to));
                        } else {
                                // Default to all for this employee or last 30 days
                                records = attendanceRepository.findByEmployeeAndDateBetween(
                                                employee, LocalDate.now().minusDays(30), LocalDate.now());
                        }

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "attendance", records));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Get team attendance (ADMIN, HR, MANAGER)
         * GET /api/attendance/team
         */
        @GetMapping("/team")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
        public ResponseEntity<?> getTeamAttendance(@RequestParam(required = false) String date,
                        Authentication auth) {
                try {
                        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();

                        String username = auth.getName();
                        Employee manager = employeeRepository.findByUserUsername(username).orElse(null);

                        List<Attendance> records;

                        // If ADMIN or HR, show all attendance
                        if (auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                                                        || a.getAuthority().equals("ROLE_HR"))) {
                                records = attendanceRepository.findByDate(targetDate);
                        } else {
                                // MANAGER - show only team attendance
                                if (manager == null) {
                                        throw new RuntimeException("Manager not found");
                                }
                                records = attendanceRepository.findByDate(targetDate).stream()
                                                .filter(att -> att.getEmployee().getReportingManager() != null &&
                                                                att.getEmployee().getReportingManager().getId()
                                                                                .equals(manager.getId()))
                                                .toList();
                        }

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "date", targetDate,
                                        "attendance", records));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Get all attendance records (ADMIN & HR)
         * GET /api/attendance/all
         */
        @GetMapping("/all")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
        public ResponseEntity<List<Attendance>> getAllAttendance(@RequestParam(required = false) String date) {
                if (date != null) {
                        LocalDate targetDate = LocalDate.parse(date);
                        return ResponseEntity.ok(attendanceRepository.findByDate(targetDate));
                }
                return ResponseEntity.ok(attendanceRepository.findAll());
        }

        /**
         * Get attendance summary (ADMIN & HR)
         * GET /api/attendance/summary
         */
        @GetMapping("/summary")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
        public ResponseEntity<?> getAttendanceSummary(@RequestParam(required = false) String date) {
                LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();

                long present = attendanceRepository.countByDateAndStatusIn(targetDate,
                                List.of(Attendance.AttendanceStatus.PRESENT,
                                                Attendance.AttendanceStatus.WORK_FROM_HOME));

                long absent = attendanceRepository.countByDateAndStatus(targetDate,
                                Attendance.AttendanceStatus.ABSENT);

                long onLeave = attendanceRepository.countByDateAndStatus(targetDate,
                                Attendance.AttendanceStatus.ON_LEAVE);

                return ResponseEntity.ok(Map.of(
                                "date", targetDate,
                                "present", present,
                                "absent", absent,
                                "onLeave", onLeave));
        }

        /**
         * Get daily attendance report (ADMIN & HR)
         * GET /api/attendance/daily-report
         */
        @GetMapping("/daily-report")
        @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
        public ResponseEntity<List<DailyAttendanceDTO>> getDailyAttendanceReport(
                        @RequestParam(required = false) String date) {
                LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
                return ResponseEntity.ok(attendanceService.getDailyAttendanceReport(targetDate));
        }

        /**
         * Get today's status (All authenticated users)
         * GET /api/attendance/today-status
         */
        @GetMapping("/today-status")
        public ResponseEntity<?> getTodayStatus(Authentication auth) {
                try {
                        String username = auth.getName();
                        Employee employee = employeeRepository.findByUserUsername(username)
                                        .orElseThrow(() -> new RuntimeException("Employee not found"));

                        LocalDate today = LocalDate.now();
                        Attendance todayAttendance = attendanceRepository.findByEmployeeAndDate(employee, today)
                                        .orElse(null);

                        if (todayAttendance == null) {
                                return ResponseEntity.ok(Map.of(
                                                "checkedIn", false,
                                                "message", "Not checked in yet"));
                        }

                        Map<String, Object> response = new java.util.HashMap<>();
                        response.put("checkedIn", true);
                        response.put("checkInTime", todayAttendance.getCheckInTime().toString());
                        response.put("checkOutTime",
                                        todayAttendance.getCheckOutTime() != null
                                                        ? todayAttendance.getCheckOutTime().toString()
                                                        : null);
                        response.put("status", todayAttendance.getStatus().toString());
                        response.put("workingMinutes",
                                        todayAttendance.getWorkingMinutes() != null
                                                        ? todayAttendance.getWorkingMinutes()
                                                        : 0);

                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }
}
