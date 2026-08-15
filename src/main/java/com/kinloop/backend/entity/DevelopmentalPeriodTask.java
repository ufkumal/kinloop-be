package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.DevelopmentDomain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "developmental_period_tasks")
public class DevelopmentalPeriodTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "min_age_months")
    private short minAgeMonths;
    @Column(name = "max_age_months")
    private short maxAgeMonths;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_domain")
    private DevelopmentDomain targetDomain;
}
