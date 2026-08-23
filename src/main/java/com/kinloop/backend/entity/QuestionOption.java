package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.*;
import jakarta.persistence.*;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "question_options")
public class QuestionOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id") private Question question;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false, columnDefinition = "TEXT") private String label;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Enumerated(EnumType.STRING) @Column(name = "dunn_quadrant") private DunnQuadrant dunnQuadrant;
    @JdbcTypeCode(SqlTypes.SMALLINT) @Column(name = "separation_anxiety") private Integer separationAnxiety;
    @Enumerated(EnumType.STRING) @Column(name = "social_orientation") private SocialOrientation socialOrientation;
    @Enumerated(EnumType.STRING) @Column(name = "focus_band") private FocusBand focusBand;
    @Column(name = "daily_time_budget_min") private Short dailyTimeBudgetMin;
    @Column(name = "daily_time_budget_max") private Short dailyTimeBudgetMax;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "gardner_priors", columnDefinition = "jsonb") private List<GardnerPrior> gardnerPriors;
}
