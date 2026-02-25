package com.karmika.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single KPI / metric line inside a {@link GoalTemplate}.
 *
 * Predefined metric types map to the four engineering pillars:
 * DELIVERY_EXECUTION – sprint commitments, on-time delivery, SLA adherence
 * QUALITY – defect leakage, code-review defects, prod incidents
 * ENGINEERING_EXCELLENCE – code coverage, perf improvements, automation
 * COLLABORATION – 360° feedback, stakeholder satisfaction
 * CUSTOM – free-text metric defined by the manager
 */
@Entity
@Table(name = "goal_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private GoalTemplate template;

    // ── Classification ─────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricPillar pillar = MetricPillar.DELIVERY_EXECUTION;

    /**
     * Either a value from the PRESET_METRICS catalogue or CUSTOM.
     * When CUSTOM the {@link #customMetricName} field holds the label.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PresetMetric presetMetric = PresetMetric.CUSTOM;

    /** Filled only when presetMetric == CUSTOM */
    @Column(length = 300)
    private String customMetricName;

    @Column(length = 500)
    private String description;

    // ── Target ─────────────────────────────────────────────────────────

    /** e.g. "%" , "incidents/month", "score 1‒5" */
    @Column(length = 100)
    private String unit;

    /** Manager's expected target value */
    private Double targetValue;

    // ── Weight ─────────────────────────────────────────────────────────

    /**
     * Percentage weight of this metric in the final goal score (all should sum ≈
     * 100)
     */
    @Column(nullable = false)
    private Integer weightage = 10;

    /** Order within the template */
    @Column(nullable = false)
    private Integer displayOrder = 0;

    // ── Enums ──────────────────────────────────────────────────────────

    public enum MetricPillar {
        DELIVERY_EXECUTION,
        QUALITY,
        ENGINEERING_EXCELLENCE,
        COLLABORATION,
        CUSTOM
    }

    public enum PresetMetric {
        // Delivery & Execution
        SPRINT_COMMITMENT_PCT("% of sprint commitments completed", MetricPillar.DELIVERY_EXECUTION, "%"),
        ON_TIME_DELIVERY_RATE("On-time delivery rate", MetricPillar.DELIVERY_EXECUTION, "%"),
        SLA_ADHERENCE("SLA adherence", MetricPillar.DELIVERY_EXECUTION, "%"),

        // Quality
        DEFECT_LEAKAGE_RATE("Defect leakage rate", MetricPillar.QUALITY, "defects/story"),
        CODE_REVIEW_DEFECTS("Code review defects", MetricPillar.QUALITY, "count"),
        PRODUCTION_INCIDENTS("Production incidents", MetricPillar.QUALITY, "count/month"),

        // Engineering Excellence
        CODE_COVERAGE_PCT("Code coverage %", MetricPillar.ENGINEERING_EXCELLENCE, "%"),
        PERFORMANCE_IMPROVEMENTS("Performance improvements", MetricPillar.ENGINEERING_EXCELLENCE, "count"),
        AUTOMATION_CONTRIBUTION("Automation contribution", MetricPillar.ENGINEERING_EXCELLENCE, "scripts"),

        // Collaboration
        FEEDBACK_360_SCORE("360° feedback score", MetricPillar.COLLABORATION, "score 1-5"),
        STAKEHOLDER_SATISFACTION("Stakeholder satisfaction rating", MetricPillar.COLLABORATION, "score 1-5"),

        // Custom
        CUSTOM("Custom metric", MetricPillar.CUSTOM, "");

        public final String label;
        public final MetricPillar pillar;
        public final String defaultUnit;

        PresetMetric(String label, MetricPillar pillar, String defaultUnit) {
            this.label = label;
            this.pillar = pillar;
            this.defaultUnit = defaultUnit;
        }
    }
}
