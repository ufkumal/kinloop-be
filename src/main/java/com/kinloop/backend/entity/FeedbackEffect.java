package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.IntelligenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "feedback_effects")
public class FeedbackEffect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;
    @Enumerated(EnumType.STRING)
    @Column(name = "intelligence_type", nullable = false, length = 30)
    private IntelligenceType intelligenceType;
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal delta;
    @Column(name = "reversed_at")
    private OffsetDateTime reversedAt;

    public FeedbackEffect(Feedback feedback, IntelligenceType intelligenceType, BigDecimal delta) {
        this.feedback = feedback;
        this.intelligenceType = intelligenceType;
        this.delta = delta;
    }

    public void reverse() {
        if (reversedAt == null) {
            reversedAt = OffsetDateTime.now();
        }
    }
}
