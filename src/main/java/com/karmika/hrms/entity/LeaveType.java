package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leave_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Casual Leave"

    @Column(nullable = false, unique = true)
    private String code; // e.g., "CASUAL_LEAVE"

    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    // Default entitlement per year (optional configuration)
    private Integer defaultDaysPerYear = 0;
}
