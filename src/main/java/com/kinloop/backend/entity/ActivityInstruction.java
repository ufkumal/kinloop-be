package com.kinloop.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "activity_instructions")
public class ActivityInstruction {
    @Id
    @Column(name = "activity_id")
    private Long activityId;
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "activity_id")
    private Activity activity;
    @Column(columnDefinition = "TEXT")
    private String intro;
    @Column(columnDefinition = "TEXT")
    private String purpose;
    @Column(name = "why_it_matters", columnDefinition = "TEXT")
    private String whyItMatters;
    @Column(name = "easier_variation", columnDefinition = "TEXT")
    private String easierVariation;
    @Column(name = "harder_variation", columnDefinition = "TEXT")
    private String harderVariation;
    @Column(name = "observation_tip", columnDefinition = "TEXT")
    private String observationTip;
    @Column(name = "safety_notes", columnDefinition = "TEXT")
    private String safetyNotes;
    @Column(name = "cleanup_notes", columnDefinition = "TEXT")
    private String cleanupNotes;
}
