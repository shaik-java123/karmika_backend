package com.karmika.hrms.service;

import com.karmika.hrms.dto.DailyAttendanceDTO;
import com.karmika.hrms.entity.Attendance;
import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.Holiday;
import com.karmika.hrms.entity.LeaveApplication;
import com.karmika.hrms.repository.AttendanceRepository;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.HolidayRepository;
import com.karmika.hrms.repository.LeaveApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final HolidayRepository holidayRepository;

    public List<DailyAttendanceDTO> getDailyAttendanceReport(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // Fetch all active employees
        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE
                        || e.getStatus() == Employee.EmployeeStatus.ON_NOTICE)
                .collect(Collectors.toList());

        // Fetch all attendance for the date
        List<Attendance> attendances = attendanceRepository.findAll().stream()
                .filter(a -> a.getDate().equals(targetDate))
                .collect(Collectors.toList());

        // Fetch all approved leaves covering the date
        List<LeaveApplication> approvedLeaves = leaveApplicationRepository
                .findByStatus(LeaveApplication.LeaveStatus.APPROVED).stream()
                .filter(l -> (l.getStartDate().isBefore(targetDate) || l.getStartDate().equals(targetDate)) &&
                        (l.getEndDate().isAfter(targetDate) || l.getEndDate().equals(targetDate)))
                .collect(Collectors.toList());

        // Check if date is holiday
        Optional<Holiday> holidayOpt = holidayRepository.findAll().stream()
                .filter(h -> h.getDate().equals(targetDate))
                .findFirst();

        boolean isWeekend = targetDate.getDayOfWeek() == DayOfWeek.SATURDAY
                || targetDate.getDayOfWeek() == DayOfWeek.SUNDAY;

        List<DailyAttendanceDTO> report = new ArrayList<>();

        for (Employee employee : employees) {
            DailyAttendanceDTO dto = DailyAttendanceDTO.builder()
                    .id(employee.getId())
                    .employeeId(employee.getEmployeeId())
                    .fullName(employee.getFirstName() + " " + employee.getLastName())
                    .department(employee.getDepartment() != null ? employee.getDepartment().getName() : "-")
                    .designation(employee.getDesignation())
                    .isLateArrival(false)
                    .isEarlyLeaving(false)
                    .build();

            // 1. Check Attendance Record
            Optional<Attendance> attendanceOpt = attendances.stream()
                    .filter(a -> a.getEmployee().getId().equals(employee.getId()))
                    .findFirst();

            if (attendanceOpt.isPresent()) {
                Attendance att = attendanceOpt.get();
                dto.setStatus(att.getStatus().toString());
                dto.setCheckInTime(att.getCheckInTime());
                dto.setCheckOutTime(att.getCheckOutTime());
                dto.setWorkingHours(
                        att.getWorkingMinutes() != null ? String.format("%.2f Hrs", att.getWorkingMinutes() / 60.0)
                                : "-");
                dto.setLateArrival(att.getIsLateArrival() != null && att.getIsLateArrival());
                dto.setEarlyLeaving(att.getIsEarlyLeaving() != null && att.getIsEarlyLeaving());
            } else {
                // 2. Check Leave
                Optional<LeaveApplication> leaveOpt = approvedLeaves.stream()
                        .filter(l -> l.getEmployee().getId().equals(employee.getId()))
                        .findFirst();

                if (leaveOpt.isPresent()) {
                    dto.setStatus("ON_LEAVE");
                    dto.setLeaveType(leaveOpt.get().getLeaveType().getName());
                } else {
                    // 3. Check Holiday
                    if (holidayOpt.isPresent()) {
                        dto.setStatus("HOLIDAY");
                        dto.setLeaveType(holidayOpt.get().getName()); // Use leaveType field for holiday name
                    } else if (isWeekend) {
                        dto.setStatus("WEEKEND");
                    } else {
                        dto.setStatus("ABSENT");
                    }
                }
            }
            report.add(dto);
        }

        return report;
    }
}
