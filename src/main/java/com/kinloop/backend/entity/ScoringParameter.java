package com.kinloop.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "scoring_parameters")
public class ScoringParameter {
    @Id
    @Column(name = "parameter_key")
    private String key;
    @Column(nullable = false)
    private BigDecimal value;
}
