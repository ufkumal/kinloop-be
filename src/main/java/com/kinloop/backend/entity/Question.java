package com.kinloop.backend.entity;

import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.entity.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionScope scope;

    @Column(name = "min_age_months", nullable = false)
    private int minAgeMonths;

    @Column(name = "max_age_months", nullable = false)
    private int maxAgeMonths;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
