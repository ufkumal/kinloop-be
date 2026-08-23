-- v6 EK8: incomplete editorial content must never enter the matching pool.
-- Content can be assembled incrementally as DRAFT; the complete aggregate is
-- validated whenever an activity is inserted or updated as PUBLISHED.

-- The v6 document states that all 243 seed cards satisfy EK8. The original
-- base seed predates that rule, however, and contains fourteen cards without
-- a secondary Gardner intelligence and three cards with only two outcomes.
-- Bring those legacy cards up to the new publication contract first.
UPDATE activities
SET secondary_intelligence = CASE id
    WHEN 1   THEN 'VISUAL_SPATIAL'
    WHEN 3   THEN 'BODILY_KINAESTHETIC'
    WHEN 9   THEN 'NATURALISTIC'
    WHEN 13  THEN 'BODILY_KINAESTHETIC'
    WHEN 18  THEN 'INTRAPERSONAL'
    WHEN 26  THEN 'INTRAPERSONAL'
    WHEN 27  THEN 'NATURALISTIC'
    WHEN 28  THEN 'VISUAL_SPATIAL'
    WHEN 29  THEN 'INTRAPERSONAL'
    WHEN 33  THEN 'VISUAL_SPATIAL'
    WHEN 35  THEN 'LOGICAL_MATHEMATICAL'
    WHEN 38  THEN 'VISUAL_SPATIAL'
    WHEN 57  THEN 'VISUAL_SPATIAL'
    WHEN 101 THEN 'NATURALISTIC'
END
WHERE id IN (1, 3, 9, 13, 18, 26, 27, 28, 29, 33, 35, 38, 57, 101)
  AND status = 'PUBLISHED'
  AND secondary_intelligence IS NULL;

INSERT INTO activity_outcomes (activity_id, outcome) VALUES
    (3,  'Baş ve gözle nesne takibini destekler'),
    (18, 'Bağımsız oturmaya hazırlık sağlar'),
    (36, 'Başını yeni görsel yöne çevirmeyi teşvik eder')
ON CONFLICT (activity_id, outcome) DO NOTHING;

CREATE OR REPLACE FUNCTION validate_published_activity()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    easier_variation TEXT;
    harder_variation TEXT;
    step_count       INTEGER;
    outcome_count    INTEGER;
BEGIN
    IF NEW.status <> 'PUBLISHED' THEN
        RETURN NEW;
    END IF;

    IF NEW.target_domain NOT IN (
        'GROSS_MOTOR', 'FINE_MOTOR', 'LANGUAGE', 'SOCIAL_EMOTIONAL',
        'COGNITIVE', 'SENSORY', 'SELF_REGULATION'
    ) THEN
        RAISE EXCEPTION 'Cannot publish activity %: target_domain is invalid', NEW.id
            USING ERRCODE = '23514';
    END IF;

    IF NEW.target_intelligence NOT IN (
        'VERBAL_LINGUISTIC', 'LOGICAL_MATHEMATICAL', 'MUSICAL',
        'VISUAL_SPATIAL', 'BODILY_KINAESTHETIC', 'INTERPERSONAL',
        'INTRAPERSONAL', 'NATURALISTIC'
    ) THEN
        RAISE EXCEPTION 'Cannot publish activity %: target_intelligence is invalid', NEW.id
            USING ERRCODE = '23514';
    END IF;

    IF NEW.secondary_intelligence IS NULL OR NEW.secondary_intelligence NOT IN (
        'VERBAL_LINGUISTIC', 'LOGICAL_MATHEMATICAL', 'MUSICAL',
        'VISUAL_SPATIAL', 'BODILY_KINAESTHETIC', 'INTERPERSONAL',
        'INTRAPERSONAL', 'NATURALISTIC'
    ) THEN
        RAISE EXCEPTION 'Cannot publish activity %: secondary_intelligence is required and must be valid', NEW.id
            USING ERRCODE = '23514';
    END IF;

    IF NEW.target_intelligence = NEW.secondary_intelligence THEN
        RAISE EXCEPTION 'Cannot publish activity %: target and secondary intelligences must be distinct', NEW.id
            USING ERRCODE = '23514';
    END IF;

    IF NEW.difficulty NOT BETWEEN 1 AND 4 THEN
        RAISE EXCEPTION 'Cannot publish activity %: difficulty must be between 1 and 4', NEW.id
            USING ERRCODE = '23514';
    END IF;

    IF NEW.duration_minutes IS NULL OR NEW.duration_minutes <= 0 THEN
        RAISE EXCEPTION 'Cannot publish activity %: duration_minutes is required and must be positive', NEW.id
            USING ERRCODE = '23514';
    END IF;

    SELECT i.easier_variation, i.harder_variation
    INTO easier_variation, harder_variation
    FROM activity_instructions i
    WHERE i.activity_id = NEW.id;

    IF btrim(COALESCE(easier_variation, '')) = '' THEN
        RAISE EXCEPTION 'Cannot publish activity %: easier_variation is required', NEW.id
            USING ERRCODE = '23514';
    END IF;

    IF btrim(COALESCE(harder_variation, '')) = '' THEN
        RAISE EXCEPTION 'Cannot publish activity %: harder_variation is required', NEW.id
            USING ERRCODE = '23514';
    END IF;

    SELECT count(*) INTO step_count
    FROM activity_steps s
    WHERE s.activity_id = NEW.id;

    IF step_count < 4 THEN
        RAISE EXCEPTION 'Cannot publish activity %: at least 4 steps are required', NEW.id
            USING ERRCODE = '23514';
    END IF;

    SELECT count(*) INTO outcome_count
    FROM activity_outcomes o
    WHERE o.activity_id = NEW.id;

    IF outcome_count < 3 THEN
        RAISE EXCEPTION 'Cannot publish activity %: at least 3 outcomes are required', NEW.id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

-- Refuse to install the gate over already-invalid published content. The v6
-- seed contains 243 valid activities; this also catches out-of-band content.
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT count(*) INTO invalid_count
    FROM activities a
    LEFT JOIN activity_instructions i ON i.activity_id = a.id
    WHERE a.status = 'PUBLISHED'
      AND (
          a.target_domain NOT IN (
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
          OR a.target_intelligence = a.secondary_intelligence
          OR a.difficulty NOT BETWEEN 1 AND 4
          OR a.duration_minutes IS NULL
          OR a.duration_minutes <= 0
          OR btrim(COALESCE(i.easier_variation, '')) = ''
          OR btrim(COALESCE(i.harder_variation, '')) = ''
          OR (SELECT count(*) FROM activity_steps s WHERE s.activity_id = a.id) < 4
          OR (SELECT count(*) FROM activity_outcomes o WHERE o.activity_id = a.id) < 3
      );

    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Cannot install PUBLISHED activity gate: % existing activities are invalid',
            invalid_count;
    END IF;
END;
$$;

CREATE TRIGGER trg_activities_validate_published
    BEFORE INSERT OR UPDATE ON activities
    FOR EACH ROW
    EXECUTE FUNCTION validate_published_activity();
