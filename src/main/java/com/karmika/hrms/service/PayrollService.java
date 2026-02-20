package com.karmika.hrms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karmika.hrms.dto.SalarySlipDTO;
import com.karmika.hrms.entity.*;
import com.karmika.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final SalarySlipRepository salarySlipRepository;
    private final EmployeeSalaryStructureRepository salaryStructureRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate salary slip for an employee for a specific month/year
     */
    @Transactional
    public SalarySlip generateSalarySlip(Long employeeId, Integer month, Integer year, Employee generatedBy) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Check if slip already exists
        Optional<SalarySlip> existingSlip = salarySlipRepository.findByEmployeeAndMonthAndYear(employee, month, year);
        if (existingSlip.isPresent()) {
            throw new RuntimeException("Salary slip already exists for this period");
        }

        // Get employee salary structure
        List<EmployeeSalaryStructure> salaryStructure = salaryStructureRepository
                .findByEmployeeAndIsActiveTrue(employee);

        if (salaryStructure.isEmpty()) {
            throw new RuntimeException("No salary structure defined for employee");
        }

        // Calculate attendance for the month
        YearMonth yearMonth = YearMonth.of(year, month);
        int totalDaysInMonth = yearMonth.lengthOfMonth();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Get Holidays
        List<LocalDate> holidays = holidayRepository.findByDateBetweenOrderByDateAsc(startDate, endDate)
                .stream().map(Holiday::getDate).collect(Collectors.toList());

        // Get Attendance records
        List<Attendance> attendances = attendanceRepository.findAll().stream()
                .filter(a -> a.getEmployee().getId().equals(employeeId))
                .filter(a -> !a.getDate().isBefore(startDate) && !a.getDate().isAfter(endDate))
                .collect(Collectors.toList());
        Map<LocalDate, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(Attendance::getDate, a -> a, (a1, a2) -> a1));

        int presentDaysCount = 0;
        int leaveDaysCount = 0;
        int payableDays = 0;

        for (int day = 1; day <= totalDaysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            boolean isWeekend = (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                    date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY);
            boolean isHoliday = holidays.contains(date);

            if (attendanceMap.containsKey(date)) {
                Attendance a = attendanceMap.get(date);
                if (a.getStatus() == Attendance.AttendanceStatus.PRESENT ||
                        a.getStatus() == Attendance.AttendanceStatus.WORK_FROM_HOME) {
                    presentDaysCount++;
                    payableDays++;
                } else if (a.getStatus() == Attendance.AttendanceStatus.ON_LEAVE) {
                    leaveDaysCount++;
                    payableDays++; // Assume paid leave
                } else if (a.getStatus() == Attendance.AttendanceStatus.HOLIDAY ||
                        a.getStatus() == Attendance.AttendanceStatus.WEEKEND) {
                    payableDays++;
                }
                // ABSENT => not payable
            } else {
                // No record
                if (isWeekend || isHoliday) {
                    payableDays++;
                }
                // Weekday with no record => Absent/Unpaid
            }
        }

        int workingDays = totalDaysInMonth;
        int absentDays = workingDays - payableDays;

        // Calculate pro-rated salary based on payable days
        double attendanceRatio = (double) payableDays / workingDays;

        // Calculate salary
        Map<String, Double> earnings = new HashMap<>();
        Map<String, Double> deductions = new HashMap<>();
        double grossSalary = 0.0;
        double totalDeductions = 0.0;

        // Get basic salary first for percentage calculations
        Double basicSalary = salaryStructure.stream()
                .filter(s -> s.getComponent().getCode().equals("BASIC"))
                .map(EmployeeSalaryStructure::getAmount)
                .findFirst()
                .orElse(0.0);

        // Process Earnings first
        for (EmployeeSalaryStructure structure : salaryStructure) {
            SalaryComponent component = structure.getComponent();
            if (component.getType() == SalaryComponent.ComponentType.EARNING) {
                double amount = structure.getAmount(); // Trust the saved structure amount

                // Apply pro-rata
                amount = amount * attendanceRatio;
                earnings.put(component.getName(), amount);
                grossSalary += amount;
            }
        }

        // Process Deductions
        for (EmployeeSalaryStructure structure : salaryStructure) {
            SalaryComponent component = structure.getComponent();
            if (component.getType() == SalaryComponent.ComponentType.DEDUCTION) {
                double amount = structure.getAmount(); // Trust the saved structure amount

                if (component.getCalculationType() == SalaryComponent.CalculationType.PERCENTAGE_OF_GROSS) {
                    amount = grossSalary * (component.getDefaultPercentage() / 100.0);
                }

                // Other types (FIXED, PERCENTAGE_OF_BASIC) use the saved amount directly

                deductions.put(component.getName(), amount);
                totalDeductions += amount;
            }
        }

        double netSalary = grossSalary - totalDeductions;

        // Create salary slip
        SalarySlip slip = new SalarySlip();
        slip.setEmployee(employee);
        slip.setMonth(month);
        slip.setYear(year);
        slip.setWorkingDays(workingDays);
        slip.setPresentDays(presentDaysCount);
        slip.setLeaveDays(leaveDaysCount);
        slip.setAbsentDays(absentDays);
        slip.setGrossSalary(grossSalary);
        slip.setTotalDeductions(totalDeductions);
        slip.setNetSalary(netSalary);
        slip.setGeneratedBy(generatedBy);
        slip.setPaymentStatus(SalarySlip.PaymentStatus.PENDING);

        try {
            slip.setEarningsJson(objectMapper.writeValueAsString(earnings));
            slip.setDeductionsJson(objectMapper.writeValueAsString(deductions));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing salary components", e);
        }

        return salarySlipRepository.save(slip);
    }

    /**
     * Convert SalarySlip entity to DTO
     */
    public SalarySlipDTO convertToDTO(SalarySlip slip) {
        Employee employee = slip.getEmployee();

        Map<String, Double> earnings = new HashMap<>();
        Map<String, Double> deductions = new HashMap<>();

        try {
            if (slip.getEarningsJson() != null) {
                earnings = objectMapper.readValue(slip.getEarningsJson(),
                        new TypeReference<Map<String, Double>>() {
                        });
            }
            if (slip.getDeductionsJson() != null) {
                deductions = objectMapper.readValue(slip.getDeductionsJson(),
                        new TypeReference<Map<String, Double>>() {
                        });
            }
        } catch (Exception e) {
            // Log error
        }

        String monthName = java.time.Month.of(slip.getMonth())
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        return SalarySlipDTO.builder()
                .id(slip.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .designation(employee.getDesignation())
                .department(employee.getDepartment() != null ? employee.getDepartment().getName() : "-")
                .month(slip.getMonth())
                .year(slip.getYear())
                .monthYear(monthName + " " + slip.getYear())
                .bankName(employee.getBankName())
                .accountNumber(employee.getBankAccountNumber())
                .ifscCode(employee.getIfscCode())
                .workingDays(slip.getWorkingDays())
                .presentDays(slip.getPresentDays())
                .leaveDays(slip.getLeaveDays())
                .absentDays(slip.getAbsentDays())
                .earnings(earnings)
                .deductions(deductions)
                .grossSalary(slip.getGrossSalary())
                .totalDeductions(slip.getTotalDeductions())
                .netSalary(slip.getNetSalary())
                .paymentStatus(slip.getPaymentStatus().toString())
                .paymentDate(slip.getPaymentDate() != null ? slip.getPaymentDate().toString() : null)
                .remarks(slip.getRemarks())
                .generatedBy(slip.getGeneratedBy() != null
                        ? slip.getGeneratedBy().getFirstName() + " " + slip.getGeneratedBy().getLastName()
                        : null)
                .createdAt(slip.getCreatedAt() != null ? slip.getCreatedAt().toString() : null)
                // Approval fields
                .approvalStatus(slip.getApprovalStatus() != null ? slip.getApprovalStatus().toString() : null)
                .approvedBy(slip.getApprovedBy() != null
                        ? slip.getApprovedBy().getFirstName() + " " + slip.getApprovedBy().getLastName()
                        : null)
                .approvedAt(slip.getApprovedAt() != null ? slip.getApprovedAt().toString() : null)
                .approvalRemarks(slip.getApprovalRemarks())
                .rejectedBy(slip.getRejectedBy() != null
                        ? slip.getRejectedBy().getFirstName() + " " + slip.getRejectedBy().getLastName()
                        : null)
                .rejectedAt(slip.getRejectedAt() != null ? slip.getRejectedAt().toString() : null)
                .rejectionReason(slip.getRejectionReason())
                .build();
    }

    /**
     * Bulk generate salary slips for all active employees
     */
    @Transactional
    public List<SalarySlip> generateBulkSalarySlips(Integer month, Integer year, Employee generatedBy) {
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE)
                .collect(Collectors.toList());

        List<SalarySlip> generatedSlips = new ArrayList<>();

        for (Employee employee : activeEmployees) {
            try {
                // Fetch fresh employee to ensure session/transaction alignment
                Employee freshEmployee = employeeRepository.findById(employee.getId()).orElse(employee);
                SalarySlip slip = generateSalarySlip(freshEmployee.getId(), month, year, generatedBy);
                generatedSlips.add(slip);
            } catch (Exception e) {
                // Log error but continue with other employees
                System.err.println(
                        "Error generating slip for employee " + employee.getEmployeeId() + ": " + e.getMessage());
            }
        }

        return generatedSlips;
    }
}
