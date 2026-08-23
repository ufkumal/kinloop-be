CREATE TABLE child_sensory_adjustments (
    child_id               BIGINT PRIMARY KEY
        CONSTRAINT fk_child_sensory_adjustments_child
            REFERENCES children (id) ON DELETE CASCADE,
    noise_adjustment       SMALLINT NOT NULL DEFAULT 0,
    visual_adjustment      SMALLINT NOT NULL DEFAULT 0,
    movement_adjustment    SMALLINT NOT NULL DEFAULT 0,
    involvement_filter     VARCHAR(10)
        CONSTRAINT chk_child_sensory_adjustments_involvement_filter
            CHECK (involvement_filter IN ('STRICT', 'RELAXED')),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);
