-- All application consumers now use the v6 child budget, freshness,
-- attachment, level initialization, feedback-streak, and selection models.

BEGIN;

-- These columns were removed by V31 on the canonical migration path. IF
-- EXISTS also cleans databases that passed through an earlier transition.
ALTER TABLE parent_profiles
    DROP COLUMN IF EXISTS daily_time_budget_minutes;

ALTER TABLE children
    DROP COLUMN IF EXISTS daily_time_budget_minutes;

ALTER TABLE question_options
    DROP COLUMN IF EXISTS daily_time_budget_minutes;

-- Q7 is child-scoped now, and no remaining question uses the household scope.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM questions WHERE scope = 'HOUSEHOLD') THEN
        RAISE EXCEPTION 'Cannot remove legacy HOUSEHOLD scope while questions still use it';
    END IF;
END;
$$;

ALTER TABLE questions DROP CONSTRAINT chk_questions_scope;
ALTER TABLE questions
    ADD CONSTRAINT chk_questions_scope
        CHECK (scope IN ('CHILD', 'CHILD_BUDGET', 'IDENTITY', 'FEEDBACK'));

COMMENT ON COLUMN questions.scope IS
    'CHILD - adaptive questionnaire; CHILD_BUDGET - per-child budget setting; IDENTITY - child creation; FEEDBACK - activity feedback.';

-- Compatibility keys retained during the v5-to-v6 rollout. Every replacement
-- key is live in Java before this migration removes its predecessor.
DELETE FROM scoring_parameters
WHERE parameter_key IN (
    'freshness_penalty',
    'freshness_lookback_days',
    'attachment_multiplier',
    'high_separation_anxiety_threshold',
    'slot_candidate_limit',
    'explore_random_top_limit',
    'domain_initial_level',
    'harder_variation_streak_increment',
    'normal_variation_streak_increment',
    'easier_variation_streak_increment',
    'domain_level_up_streak_threshold',
    'domain_level_down_easier_threshold'
);

COMMIT;
