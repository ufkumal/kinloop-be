-- Align activity difficulty and child domain levels with the v5 four-step
-- model. Level 5 has no defined activity difficulty, so any legacy level-5
-- state and content are capped before the new constraints are installed.
UPDATE activities SET difficulty = 4 WHERE difficulty = 5;
UPDATE child_domain_levels SET level = 4 WHERE level = 5;

ALTER TABLE activities DROP CONSTRAINT activities_difficulty_check;
ALTER TABLE activities
    ADD CONSTRAINT chk_activities_difficulty CHECK (difficulty BETWEEN 1 AND 4);

ALTER TABLE child_domain_levels DROP CONSTRAINT child_domain_levels_level_check;
ALTER TABLE child_domain_levels
    ADD CONSTRAINT chk_child_domain_levels_level CHECK (level BETWEEN 1 AND 4);
