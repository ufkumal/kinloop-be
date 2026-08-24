ALTER TABLE children
    ADD COLUMN daily_time_budget_answered_at TIMESTAMPTZ;

-- Completed profiles necessarily passed this screen. This keeps existing users
-- out of onboarding while leaving incomplete default-valued rows conservative.
UPDATE children
SET daily_time_budget_answered_at = COALESCE(onboarding_completed_at, updated_at)
WHERE onboarding_completed_at IS NOT NULL
   OR (daily_time_budget_min, daily_time_budget_max) <> (25, 35);

COMMENT ON COLUMN children.daily_time_budget_answered_at IS
    'Presence means the parent explicitly submitted the child budget screen; defaults alone are not progress.';
