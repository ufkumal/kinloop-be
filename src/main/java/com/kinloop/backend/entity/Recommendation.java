package com.kinloop.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "recommendations")
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "child_id")
    private Long childId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;
    @Column
    private BigDecimal score;
    @Column
    private Short rank;
    @Column(nullable = false)
    private String source = "RULE";
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_breakdown", columnDefinition = "jsonb")
    private Map<String, Object> scoreBreakdown;

    public Recommendation(Long childId, Activity activity, BigDecimal score, short rank, Map<String, Object> breakdown) {
        this.childId = childId;
        this.activity = activity;
        this.score = score;
        this.rank = rank;
        this.scoreBreakdown = breakdown;
    }
}
