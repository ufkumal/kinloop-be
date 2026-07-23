package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "child_profile_snapshots")
public class ChildProfileSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "child_id", nullable = false) private Long childId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id") private QuestionnaireSession session;
    @Enumerated(EnumType.STRING) @Column(name = "age_band", nullable = false) private AgeBand ageBand;
    @JdbcTypeCode(SqlTypes.SMALLINT) @Column(name = "age_months_at_capture", nullable = false) private Integer ageMonthsAtCapture;
    @Enumerated(EnumType.STRING) @Column(name = "dunn_quadrant") private DunnQuadrant dunnQuadrant;
    @JdbcTypeCode(SqlTypes.SMALLINT) @Column(name = "noise_sensitivity") private Integer noiseSensitivity;
    @JdbcTypeCode(SqlTypes.SMALLINT) @Column(name = "visual_sensitivity") private Integer visualSensitivity;
    @JdbcTypeCode(SqlTypes.SMALLINT) @Column(name = "mobility_preference") private Integer mobilityPreference;
    @JdbcTypeCode(SqlTypes.SMALLINT) @Column(name = "separation_anxiety") private Integer separationAnxiety;
    @Enumerated(EnumType.STRING) @Column(name = "social_orientation") private SocialOrientation socialOrientation;
    @Enumerated(EnumType.STRING) @Column(name = "focus_band") private FocusBand focusBand;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "gardner_priors", columnDefinition = "jsonb") private List<GardnerPrior> gardnerPriors;
    @Column(name = "is_current", nullable = false) private boolean current;
    @Column(name = "captured_at", insertable = false, updatable = false) private OffsetDateTime capturedAt;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private OffsetDateTime updatedAt;
}
