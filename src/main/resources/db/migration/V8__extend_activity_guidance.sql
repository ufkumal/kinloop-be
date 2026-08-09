-- Extend parent-facing activity guidance with structured pedagogical content.
ALTER TABLE activity_instructions
    ADD COLUMN purpose            TEXT,
    ADD COLUMN why_it_matters     TEXT,
    ADD COLUMN easier_variation   TEXT,
    ADD COLUMN harder_variation   TEXT,
    ADD COLUMN observation_tip    TEXT;

-- An activity may communicate several concise, ordered developmental outcomes.
CREATE TABLE activity_outcomes (
    id            BIGSERIAL PRIMARY KEY,
    activity_id   BIGINT   NOT NULL
        CONSTRAINT fk_activity_outcomes_activity
            REFERENCES activities (id) ON DELETE CASCADE,
    outcome       TEXT     NOT NULL,
    display_order SMALLINT NOT NULL CHECK (display_order > 0),

    CONSTRAINT uq_activity_outcomes_activity_outcome
        UNIQUE (activity_id, outcome),
    CONSTRAINT uq_activity_outcomes_activity_order
        UNIQUE (activity_id, display_order)
);

CREATE INDEX idx_activity_outcomes_activity_id
    ON activity_outcomes (activity_id);
