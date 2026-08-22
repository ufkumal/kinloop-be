-- Finalizes the v6 migration after V19 and V20 were already applied.
-- Applied Flyway migrations are immutable; corrections belong in this
-- forward-only migration rather than changing their recorded checksums.

BEGIN;

-- v6 section 1.2: every Dunn profile, including C4, must have real weights.
-- V20 populated C4 after dropping the old NULL-mode constraint. Make the new
-- invariant structural so later updates cannot reintroduce NULL weights.
ALTER TABLE dunn_profiles
    ALTER COLUMN noise_weight SET NOT NULL,
    ALTER COLUMN visual_weight SET NOT NULL,
    ALTER COLUMN movement_weight SET NOT NULL;

-- v6 section 1.4: all children that existed at the v6 transition receive
-- option B. A child may choose A/B/C during the next re-onboarding flow.
UPDATE children
SET daily_time_budget_min = 25,
    daily_time_budget_max = 35;

-- v6 section 1.6: persist only valid budget snapshots and duration totals.
ALTER TABLE daily_plans
    ADD CONSTRAINT chk_daily_plans_budget_range
        CHECK ((budget_min, budget_max) IN ((15, 25), (25, 35), (35, 45))),
    ADD CONSTRAINT chk_daily_plans_committed_duration
        CHECK (committed_duration_minutes >= 0 AND committed_duration_minutes <= budget_max),
    ADD CONSTRAINT chk_daily_plans_total_duration
        CHECK (total_duration_minutes >= committed_duration_minutes);

-- V19 pool verification: exact seed counts plus the v6 EK8 fields that are
-- already required for ids 163-243. The repository-wide publication gate is
-- implemented separately because legacy V10 content still needs remediation.
DO $$
DECLARE
    activity_count     INT;
    instruction_count  INT;
    step_count         INT;
    material_count     INT;
    outcome_count      INT;
    invalid_count      INT;
BEGIN
    SELECT count(*) INTO activity_count
    FROM activities
    WHERE id BETWEEN 163 AND 243;

    SELECT count(*) INTO instruction_count
    FROM activity_instructions
    WHERE activity_id BETWEEN 163 AND 243;

    SELECT count(*) INTO step_count
    FROM activity_steps
    WHERE activity_id BETWEEN 163 AND 243;

    SELECT count(*) INTO material_count
    FROM activity_materials
    WHERE activity_id BETWEEN 163 AND 243;

    SELECT count(*) INTO outcome_count
    FROM activity_outcomes
    WHERE activity_id BETWEEN 163 AND 243;

    IF activity_count <> 81 THEN
        RAISE EXCEPTION 'Expected 81 V19 activities, found %', activity_count;
    END IF;
    IF instruction_count <> 81 THEN
        RAISE EXCEPTION 'Expected 81 V19 activity instructions, found %', instruction_count;
    END IF;
    IF step_count <> 405 THEN
        RAISE EXCEPTION 'Expected 405 V19 activity steps, found %', step_count;
    END IF;
    IF material_count <> 119 THEN
        RAISE EXCEPTION 'Expected 119 V19 activity materials, found %', material_count;
    END IF;
    IF outcome_count <> 243 THEN
        RAISE EXCEPTION 'Expected 243 V19 activity outcomes, found %', outcome_count;
    END IF;

    SELECT count(*) INTO invalid_count
    FROM activities a
    LEFT JOIN activity_instructions i ON i.activity_id = a.id
    WHERE a.id BETWEEN 163 AND 243
      AND (
          a.status <> 'PUBLISHED'
          OR a.scope <> 'HOME'
          OR a.deleted_at IS NOT NULL
          OR a.target_domain NOT IN (
              'GROSS_MOTOR', 'FINE_MOTOR', 'LANGUAGE', 'SOCIAL_EMOTIONAL',
              'COGNITIVE', 'SENSORY', 'SELF_REGULATION'
          )
          OR a.target_intelligence NOT IN (
              'VERBAL_LINGUISTIC', 'LOGICAL_MATHEMATICAL', 'MUSICAL',
              'VISUAL_SPATIAL', 'BODILY_KINAESTHETIC', 'INTERPERSONAL',
              'INTRAPERSONAL', 'NATURALISTIC'
          )
          OR a.secondary_intelligence IS NULL
          OR a.secondary_intelligence NOT IN (
              'VERBAL_LINGUISTIC', 'LOGICAL_MATHEMATICAL', 'MUSICAL',
              'VISUAL_SPATIAL', 'BODILY_KINAESTHETIC', 'INTERPERSONAL',
              'INTRAPERSONAL', 'NATURALISTIC'
          )
          OR a.secondary_intelligence = a.target_intelligence
          OR a.difficulty NOT BETWEEN 1 AND 4
          OR a.duration_minutes IS NULL
          OR a.duration_minutes <= 0
          OR GREATEST(a.noise_load, a.visual_load) > 2
          OR a.involvement_type = 'BAGIMSIZ'
          OR btrim(COALESCE(i.easier_variation, '')) = ''
          OR btrim(COALESCE(i.harder_variation, '')) = ''
          OR (SELECT count(*) FROM activity_steps s WHERE s.activity_id = a.id) < 4
          OR (SELECT count(*) FROM activity_outcomes o WHERE o.activity_id = a.id) < 3
      );

    IF invalid_count <> 0 THEN
        RAISE EXCEPTION '% V19 activities failed v6 publication/pool validation', invalid_count;
    END IF;
END $$;

COMMIT;
