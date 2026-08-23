-- The reconstructed V1 baseline already includes this column. Keep this
-- migration safe for both that baseline and installations where it is absent.
ALTER TABLE feedback_effects
    ADD COLUMN IF NOT EXISTS reversed_at TIMESTAMPTZ;
