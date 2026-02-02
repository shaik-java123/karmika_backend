package com.karmika.hrms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class DailyAttendanceDTO {
    private Long id;
    private String employeeId; // The ID string, e.g. EMP001
    private String fullName;
    private String department;
    private String designation;
    private String status; // Present, Absent, On Leave, Holiday, Weekend
    private String leaveType; // if status is On Leave
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String workingHours;
    private boolean isLateArrival;
    private boolean isEarlyLeaving;
}
