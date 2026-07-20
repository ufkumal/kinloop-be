package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.AgeBand;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.entity.enums.TriggerReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questionnaire_sessions")
public class QuestionnaireSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_band", nullable = false, length = 16)
    private AgeBand ageBand;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "age_months_at_start", nullable = false)
    private int ageMonthsAtStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false, length = 16)
    private TriggerReason triggerReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public static QuestionnaireSession open(Long childId, int ageMonths, TriggerReason triggerReason) {
        QuestionnaireSession session = new QuestionnaireSession();
        session.setChildId(childId);
        session.setAgeBand(AgeBand.resolve(ageMonths));
        session.setAgeMonthsAtStart(ageMonths);
        session.setTriggerReason(triggerReason);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(OffsetDateTime.now());
        return session;
    }
}
