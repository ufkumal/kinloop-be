package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.*;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "activities")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String scope;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "min_age_months", nullable = false)
    private int minAgeMonths;
    @Column(name = "max_age_months", nullable = false)
    private int maxAgeMonths;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_intelligence", nullable = false)
    private IntelligenceType targetIntelligence;
    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_intelligence")
    private IntelligenceType secondaryIntelligence;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_domain", nullable = false)
    private DevelopmentDomain targetDomain;
    @Column(nullable = false)
    private short difficulty;
    @Column(name = "duration_minutes", nullable = false)
    private short durationMinutes;
    @Enumerated(EnumType.STRING)
    @Column(name = "involvement_type", nullable = false)
    private InvolvementType involvementType;
    @Column(name = "noise_load", nullable = false)
    private short noiseLoad;
    @Column(name = "visual_load", nullable = false)
    private short visualLoad;
    @Column(name = "physical_intensity", nullable = false)
    private short physicalIntensity;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    @OneToOne(mappedBy = "activity")
    private ActivityInstruction instruction;
    @OneToMany(mappedBy = "activity", fetch = FetchType.LAZY)
    @OrderBy("stepNo ASC")
    private Set<ActivityStep> steps = new LinkedHashSet<>();
    @OneToMany(mappedBy = "activity", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    private Set<ActivityMaterial> materials = new LinkedHashSet<>();
    @OneToMany(mappedBy = "activity", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, id ASC")
    private Set<ActivityOutcome> outcomes = new LinkedHashSet<>();
}
