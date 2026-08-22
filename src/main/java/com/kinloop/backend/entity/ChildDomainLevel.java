package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.DevelopmentDomain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "child_domain_levels", uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "domain"}))
public class ChildDomainLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "child_id", nullable = false)
    private Long childId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DevelopmentDomain domain;
    @Column(nullable = false)
    private short level;
    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal streak = BigDecimal.ZERO;

    public ChildDomainLevel(Long childId, DevelopmentDomain domain, short level) {
        this.childId = childId;
        this.domain = domain;
        this.level = level;
    }

    public void applyFeedback(
            BigDecimal delta,
            BigDecimal levelUpThreshold,
            BigDecimal levelDownThreshold,
            short levelMin,
            short levelMax,
            BigDecimal ceilingCounterCap
    ) {
        streak = streak.add(delta);
        if (level == levelMax && streak.compareTo(ceilingCounterCap) > 0) {
            streak = ceilingCounterCap;
        }
        if (level < levelMax && streak.compareTo(levelUpThreshold) >= 0) {
            level++;
            streak = BigDecimal.ZERO;
        } else if (streak.compareTo(levelDownThreshold) < 0) {
            if (level > levelMin) level--;
            streak = BigDecimal.ZERO;
        }
    }
}
