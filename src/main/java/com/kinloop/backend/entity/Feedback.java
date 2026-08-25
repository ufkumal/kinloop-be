package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.FeedbackReason;
import com.kinloop.backend.entity.enums.FeedbackType;
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
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "child_id", nullable = false)
    private Long childId;
    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity activity;
    @ManyToOne(optional = false)
    @JoinColumn(name = "daily_plan_item_id", nullable = false)
    private DailyPlanItem dailyPlanItem;
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", length = 15)
    private FeedbackType feedbackType;
    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_reason", length = 15)
    private FeedbackReason resolvedReason;
    @Size(max = 500)
    @Column(name = "free_text", columnDefinition = "TEXT")
    private String freeText;
    @Column(nullable = false)
    private boolean accepted = true;
    @Column(name = "bulk_flag", nullable = false)
    private boolean bulkFlag;
    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Feedback(
            Long childId,
            DailyPlanItem dailyPlanItem,
            FeedbackType feedbackType,
            FeedbackReason resolvedReason,
            String freeText
    ) {
        this.childId = childId;
        this.dailyPlanItem = dailyPlanItem;
        this.activity = dailyPlanItem.getActivity();
        this.feedbackType = feedbackType;
        this.resolvedReason = resolvedReason;
        this.freeText = freeText;
    }
}
