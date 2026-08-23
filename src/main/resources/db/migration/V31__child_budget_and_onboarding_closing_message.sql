-- -14: the time commitment belongs to each child and is a range.
-- -15: persist the one-time closing-message decision per child.

BEGIN;

ALTER TABLE question_options
    ADD COLUMN daily_time_budget_min SMALLINT,
    ADD COLUMN daily_time_budget_max SMALLINT;

ALTER TABLE questions DROP CONSTRAINT chk_questions_scope;
ALTER TABLE questions
    ADD CONSTRAINT chk_questions_scope
        CHECK (scope IN ('CHILD', 'HOUSEHOLD', 'IDENTITY', 'FEEDBACK', 'CHILD_BUDGET'));

UPDATE questions
SET body = 'Çocuğunuzla etkinlik için genellikle ne kadar vakit ayırabiliyorsunuz?',
    scope = 'CHILD_BUDGET'
WHERE code = 'Q7';

UPDATE question_options qo
SET label = CASE qo.code
                WHEN 'A' THEN 'Kısa ve öz olsun'
                WHEN 'B' THEN 'Yarım saatim var'
                WHEN 'C' THEN 'Rahatça vakit ayırabiliriz'
            END,
    daily_time_budget_min = CASE qo.code WHEN 'A' THEN 15 WHEN 'B' THEN 25 WHEN 'C' THEN 35 END,
    daily_time_budget_max = CASE qo.code WHEN 'A' THEN 25 WHEN 'B' THEN 35 WHEN 'C' THEN 45 END
FROM questions q
WHERE qo.question_id = q.id
  AND q.code = 'Q7';

ALTER TABLE question_options
    DROP CONSTRAINT chk_qo_time_budget,
    DROP COLUMN daily_time_budget_minutes,
    ADD CONSTRAINT chk_qo_time_budget_range CHECK (
        (daily_time_budget_min IS NULL AND daily_time_budget_max IS NULL)
        OR (daily_time_budget_min, daily_time_budget_max) IN ((15, 25), (25, 35), (35, 45))
    );

ALTER TABLE parent_profiles
    DROP CONSTRAINT chk_parent_profiles_time_budget,
    DROP COLUMN daily_time_budget_minutes;

ALTER TABLE children
    ADD COLUMN onboarding_closing_message_responded_at TIMESTAMPTZ,
    ADD COLUMN onboarding_closing_reminder_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN onboarding_closing_reminder_plan_baseline INT;

ALTER TABLE children
    ADD CONSTRAINT chk_children_closing_reminder_state CHECK (
        (onboarding_closing_reminder_requested = FALSE
            AND onboarding_closing_reminder_plan_baseline IS NULL)
        OR (onboarding_closing_reminder_requested = TRUE
            AND onboarding_closing_message_responded_at IS NOT NULL
            AND onboarding_closing_reminder_plan_baseline >= 0)
    );

COMMENT ON COLUMN questions.scope IS
    'CHILD - adaptive questionnaire; CHILD_BUDGET - per-child budget setting; HOUSEHOLD - legacy household setting; IDENTITY - child creation; FEEDBACK - activity feedback.';

COMMIT;
