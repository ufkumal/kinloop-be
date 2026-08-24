-- Preserve completed selection history while allowing the next activity in the
-- same daily plan to become active. Only unfinished selections are exclusive.
DROP INDEX uq_daily_plan_items_selected_per_plan;

CREATE UNIQUE INDEX uq_daily_plan_items_selected_per_plan
    ON daily_plan_items (daily_plan_id)
    WHERE selected_at IS NOT NULL
      AND completed_at IS NULL;
