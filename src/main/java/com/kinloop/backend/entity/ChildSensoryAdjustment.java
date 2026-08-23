package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.InvolvementFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "child_sensory_adjustments")
public class ChildSensoryAdjustment {
    @Id
    @Column(name = "child_id")
    private Long childId;
    @Column(name = "noise_adjustment", nullable = false)
    private short noiseAdjustment;
    @Column(name = "visual_adjustment", nullable = false)
    private short visualAdjustment;
    @Column(name = "movement_adjustment", nullable = false)
    private short movementAdjustment;
    @Enumerated(EnumType.STRING)
    @Column(name = "involvement_filter", length = 10)
    private InvolvementFilter involvementFilter;
    @Generated(event = EventType.INSERT)
    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public ChildSensoryAdjustment(
            Long childId,
            short noiseAdjustment,
            short visualAdjustment,
            short movementAdjustment,
            InvolvementFilter involvementFilter
    ) {
        this.childId = childId;
        this.noiseAdjustment = noiseAdjustment;
        this.visualAdjustment = visualAdjustment;
        this.movementAdjustment = movementAdjustment;
        this.involvementFilter = involvementFilter;
    }
}
