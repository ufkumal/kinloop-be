package com.kinloop.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "parent_profiles")
public class ParentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column
    private String phone;

    @Column
    private String city;

    @Column
    private String district;

    @Column(name = "daily_time_budget_minutes")
    private Short dailyTimeBudgetMinutes;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
