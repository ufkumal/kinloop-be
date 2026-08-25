-- A child may receive another three-activity round on the same day after all
-- items in the latest round have received feedback.
ALTER TABLE daily_plans
    DROP CONSTRAINT uq_daily_plans_child_date;

-- Latest-round lookup and freshness history both order same-day plans by id.
DROP INDEX idx_daily_plans_child;

CREATE INDEX idx_daily_plans_child
    ON daily_plans (child_id, plan_date DESC, id DESC);
