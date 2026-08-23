CREATE TABLE feedback_llm_classifications (
    feedback_id        BIGINT PRIMARY KEY
        CONSTRAINT fk_feedback_llm_classification_feedback
            REFERENCES feedback (id) ON DELETE CASCADE,
    applied            BOOLEAN NOT NULL DEFAULT FALSE,
    confidence         NUMERIC(3,2),
    target_correction  VARCHAR(30)
        CONSTRAINT chk_feedback_llm_target_correction
            CHECK (target_correction IN (
                'VERBAL_LINGUISTIC', 'LOGICAL_MATHEMATICAL', 'VISUAL_SPATIAL',
                'BODILY_KINAESTHETIC', 'MUSICAL', 'INTERPERSONAL',
                'INTRAPERSONAL', 'NATURALISTIC')),
    secondary_hint     VARCHAR(30)
        CONSTRAINT chk_feedback_llm_secondary_hint
            CHECK (secondary_hint IN (
                'VERBAL_LINGUISTIC', 'LOGICAL_MATHEMATICAL', 'VISUAL_SPATIAL',
                'BODILY_KINAESTHETIC', 'MUSICAL', 'INTERPERSONAL',
                'INTRAPERSONAL', 'NATURALISTIC')),
    sensory_hint       VARCHAR(20)
        CONSTRAINT chk_feedback_llm_sensory_hint
            CHECK (sensory_hint IN ('NOISE', 'VISUAL', 'MOVEMENT', 'CROWDING')),
    involvement_hint   VARCHAR(20)
        CONSTRAINT chk_feedback_llm_involvement_hint
            CHECK (involvement_hint IN ('TOGETHER', 'ALONE')),
    difficulty_hint    VARCHAR(10)
        CONSTRAINT chk_feedback_llm_difficulty_hint
            CHECK (difficulty_hint IN ('HARDER', 'EASIER')),
    situation_hint     VARCHAR(10)
        CONSTRAINT chk_feedback_llm_situation_hint
            CHECK (situation_hint IN ('TRANSIENT')),
    duration_hint      VARCHAR(10)
        CONSTRAINT chk_feedback_llm_duration_hint
            CHECK (duration_hint IN ('LONG', 'SHORT')),
    conflict           BOOLEAN NOT NULL DEFAULT FALSE,
    raw_response       TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
