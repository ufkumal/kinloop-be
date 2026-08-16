ALTER TABLE daily_plan_items
    ADD COLUMN selected_at TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_daily_plan_items_selected_per_plan
    ON daily_plan_items (daily_plan_id)
    WHERE selected_at IS NOT NULL;
