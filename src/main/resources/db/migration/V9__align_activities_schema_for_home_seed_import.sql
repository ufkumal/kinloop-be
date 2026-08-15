-- =====================================================================
-- V9__align_activities_schema_for_home_seed_import.sql
-- Prepares the schema for the 128-card Kidloop HOME activity seed
-- (Kidloop_128_Statement_Master.xlsx). Diffing that insert script
-- against the schema below found three blocking gaps:
--
--   1) activities.target_domain only allowed 5 values. The seed uses
--      two more: SENSORY (17 cards) and SELF_REGULATION (8 cards).
--   2) The seed writes to activity_steps(activity_id, step_no, text),
--      but that table exists here as
--      activity_instruction_steps(activity_id, step_order, body).
--      Renamed to match the plain naming already used by its sibling
--      tables (activity_materials, activity_outcomes) and by the seed.
--   3) activity_outcomes.display_order is NOT NULL with no default,
--      but the seed inserts (activity_id, outcome) only. Backfilled
--      with a monotonic sequence default: rows for one activity are
--      always inserted consecutively, so the sequence still preserves
--      per-activity display order even though values aren't 1-based.
--
-- Checked and NOT changed:
--   - target_intelligence / secondary_intelligence already accept
--     VERBAL_LINGUISTIC / VISUAL_SPATIAL (not LINGUISTIC / SPATIAL);
--     every value the seed uses already fits the existing CHECK.
--   - difficulty already allows 1-5; the seed's max value is 4, so the
--     existing range is a safe superset (left at 1-5 since other
--     WORKSHOP-scope content may still use 5).
-- =====================================================================

-- 1) target_domain: widen CHECK to the 7-domain set used by the seed.
ALTER TABLE activities DROP CONSTRAINT activities_target_domain_check;
ALTER TABLE activities
    ADD CONSTRAINT chk_activities_target_domain
        CHECK (target_domain IN ('GROSS_MOTOR', 'FINE_MOTOR', 'LANGUAGE',
            'SOCIAL_EMOTIONAL', 'COGNITIVE', 'SENSORY', 'SELF_REGULATION'));

-- 2) activity_instruction_steps -> activity_steps, matching the naming
--    already used by activity_materials / activity_outcomes.
ALTER TABLE activity_instruction_steps RENAME TO activity_steps;
ALTER TABLE activity_steps RENAME COLUMN step_order TO step_no;
ALTER TABLE activity_steps RENAME COLUMN body TO text;

-- 3) activity_outcomes.display_order: default to a monotonic sequence
--    so a plain (activity_id, outcome) insert still satisfies NOT NULL
--    and the per-activity uniqueness constraint.
CREATE SEQUENCE activity_outcomes_display_order_seq
    OWNED BY activity_outcomes.display_order;
ALTER TABLE activity_outcomes
    ALTER COLUMN display_order SET DEFAULT nextval('activity_outcomes_display_order_seq');
