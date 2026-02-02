package com.karmika.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalarySlipDTO {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String designation;
    private String department;
    private Integer month;
    private Integer year;
    private String monthYear; // e.g., "January 2026"

    // Bank Details
    private String bankName;
    private String accountNumber;
    private String ifscCode;

    // Attendance
    private Integer workingDays;
    private Integer presentDays;
    private Integer leaveDays;
    private Integer absentDays;

    // Salary Breakdown
    private Map<String, Double> earnings; // Component name -> Amount
    private Map<String, Double> deductions; // Component name -> Amount

    private Double grossSalary;
    private Double totalDeductions;
    private Double netSalary;

    private String paymentStatus;
    private String paymentDate;
    private String remarks;
    private String generatedBy;
    private String createdAt;

    // Approval Workflow Fields
    private String approvalStatus;
    private String approvedBy;
    private String approvedAt;
    private String approvalRemarks;
    private String rejectedBy;
    private String rejectedAt;
    private String rejectionReason;
}
