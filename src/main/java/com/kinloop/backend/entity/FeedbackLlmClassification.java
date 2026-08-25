package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.DifficultyHint;
import com.kinloop.backend.entity.enums.DurationHint;
import com.kinloop.backend.entity.enums.IntelligenceType;
import com.kinloop.backend.entity.enums.InvolvementHint;
import com.kinloop.backend.entity.enums.SensoryHint;
import com.kinloop.backend.entity.enums.SituationHint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "feedback_llm_classifications")
public class FeedbackLlmClassification {
    @Id
    @Column(name = "feedback_id")
    private Long feedbackId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;
    @Column(nullable = false)
    private boolean applied;
    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_correction", length = 30)
    private IntelligenceType targetCorrection;
    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_hint", length = 30)
    private IntelligenceType secondaryHint;
    @Enumerated(EnumType.STRING)
    @Column(name = "sensory_hint", length = 20)
    private SensoryHint sensoryHint;
    @Enumerated(EnumType.STRING)
    @Column(name = "involvement_hint", length = 20)
    private InvolvementHint involvementHint;
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_hint", length = 10)
    private DifficultyHint difficultyHint;
    @Enumerated(EnumType.STRING)
    @Column(name = "situation_hint", length = 10)
    private SituationHint situationHint;
    @Enumerated(EnumType.STRING)
    @Column(name = "duration_hint", length = 10)
    // Stored for future use; plan generation does not consume duration hints yet.
    private DurationHint durationHint;
    @Column(nullable = false)
    private boolean conflict;
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;
    @Column(name = "model_name", length = 50)
    private String modelName;
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public FeedbackLlmClassification(
            Feedback feedback,
            String modelName,
            String rawResponse,
            BigDecimal confidence,
            IntelligenceType targetCorrection,
            IntelligenceType secondaryHint,
            SensoryHint sensoryHint,
            InvolvementHint involvementHint,
            DifficultyHint difficultyHint,
            SituationHint situationHint,
            DurationHint durationHint,
            boolean conflict
    ) {
        this.feedback = feedback;
        // @MapsId copies the feedback ID during persist. Leaving feedbackId null here
        // also lets Spring Data recognize this instance as new instead of merging it.
        this.modelName = modelName;
        this.rawResponse = rawResponse;
        this.confidence = confidence;
        this.targetCorrection = targetCorrection;
        this.secondaryHint = secondaryHint;
        this.sensoryHint = sensoryHint;
        this.involvementHint = involvementHint;
        this.difficultyHint = difficultyHint;
        this.situationHint = situationHint;
        this.durationHint = durationHint;
        this.conflict = conflict;
    }

    public void markApplied() {
        this.applied = true;
    }
}
