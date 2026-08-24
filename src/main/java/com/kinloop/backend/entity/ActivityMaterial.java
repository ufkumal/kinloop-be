package com.kinloop.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "activity_materials")
public class ActivityMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(length = 50)
    private String category;
    @Column(length = 50)
    private String quantity;
    @Column(name = "is_optional", nullable = false)
    private boolean optional;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(columnDefinition = "TEXT")
    private String note;
}
