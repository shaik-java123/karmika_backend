package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_slips")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SalarySlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer month; // 1-12

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer workingDays;

    @Column(nullable = false)
    private Integer presentDays;

    private Integer leaveDays;

    private Integer absentDays;

    @Column(nullable = false)
    private Double grossSalary;

    @Column(nullable = false)
    private Double totalDeductions;

    @Column(nullable = false)
    private Double netSalary;

    @Column(length = 2000)
    private String earningsJson; // JSON string of earnings breakdown

    @Column(length = 2000)
    private String deductionsJson; // JSON string of deductions breakdown

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDate paymentDate;

    @Column(length = 500)
    private String remarks;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "generated_by")
    private Employee generatedBy;

    // Approval Workflow Fields
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING_APPROVAL;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String approvalRemarks;

    @ManyToOne
    @JoinColumn(name = "rejected_by")
    private Employee rejectedBy;

    private LocalDateTime rejectedAt;

    @Column(length = 500)
    private String rejectionReason;

    public enum PaymentStatus {
        PENDING,
        PROCESSED,
        PAID,
        CANCELLED
    }

    public enum ApprovalStatus {
        PENDING_APPROVAL, // Generated, waiting for approval
        APPROVED, // Approved by authorized person
        REJECTED, // Rejected, needs correction
        CANCELLED // Cancelled by HR/Admin
    }
}
