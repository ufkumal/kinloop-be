package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.IntelligenceType;
import jakarta.persistence.*;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "child_intelligence_scores", uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "intelligence_type"}))
public class ChildIntelligenceScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "child_id", nullable = false)
    private Long childId;
    @Enumerated(EnumType.STRING)
    @Column(name = "intelligence_type", nullable = false)
    private IntelligenceType intelligenceType;
    @Column(nullable = false)
    private BigDecimal score;
    @Column(name = "feedback_count", nullable = false)
    private int feedbackCount;

    public ChildIntelligenceScore(Long childId, IntelligenceType type, BigDecimal score) {
        this.childId = childId;
        this.intelligenceType = type;
        this.score = score;
    }

    public void applyPrior(BigDecimal delta, BigDecimal min, BigDecimal max) {
        score = score.add(delta).max(min).min(max);
    }

    public BigDecimal applyFeedback(BigDecimal delta, BigDecimal min, BigDecimal max) {
        BigDecimal previous = score;
        score = score.add(delta).max(min).min(max);
        return score.subtract(previous);
    }

    public void recordFeedbackSample() {
        feedbackCount++;
    }
}
