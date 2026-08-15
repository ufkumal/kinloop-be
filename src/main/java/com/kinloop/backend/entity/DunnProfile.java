package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.DunnQuadrant;
import jakarta.persistence.*;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "dunn_profiles")
public class DunnProfile {
    @Id
    @Enumerated(EnumType.STRING)
    private DunnQuadrant quadrant;
    @Column(name = "noise_tolerance")
    private short noiseTolerance;
    @Column(name = "visual_tolerance")
    private short visualTolerance;
    @Column(name = "movement_tolerance")
    private short movementTolerance;
    @Column(name = "noise_weight")
    private BigDecimal noiseWeight;
    @Column(name = "visual_weight")
    private BigDecimal visualWeight;
    @Column(name = "movement_weight")
    private BigDecimal movementWeight;
}
