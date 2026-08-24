package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.Gender;
import com.kinloop.backend.entity.enums.PreferenceMode;
import com.kinloop.backend.entity.enums.AgeBand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "children")
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_mode", nullable = false, length = 15)
    private PreferenceMode preferenceMode = PreferenceMode.BALANCED;

    @Column(name = "daily_time_budget_min", nullable = false)
    private short dailyTimeBudgetMin = 25;

    @Column(name = "daily_time_budget_max", nullable = false)
    private short dailyTimeBudgetMax = 35;

    @Column(name = "daily_time_budget_answered_at")
    private OffsetDateTime dailyTimeBudgetAnsweredAt;

    @Column(name = "onboarding_completed_at")
    private OffsetDateTime onboardingCompletedAt;

    @Column(name = "onboarding_closing_message_responded_at")
    private OffsetDateTime onboardingClosingMessageRespondedAt;

    @Column(name = "onboarding_closing_reminder_requested", nullable = false)
    private boolean onboardingClosingReminderRequested;

    @Column(name = "onboarding_closing_reminder_plan_baseline")
    private Integer onboardingClosingReminderPlanBaseline;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public int ageInMonths(LocalDate on) {
        return AgeBand.ageInMonths(birthDate, on);
    }
}
