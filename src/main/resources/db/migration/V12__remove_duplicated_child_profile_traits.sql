-- Sensory tolerances and weights now come exclusively from dunn_profiles by
-- child_profile_snapshots.dunn_quadrant. Q3 remains preserved as the selected
-- option code in child_answers but has no matching-engine effect yet.

ALTER TABLE child_profile_snapshots
    DROP COLUMN noise_sensitivity,
    DROP COLUMN visual_sensitivity,
    DROP COLUMN mobility_preference;

ALTER TABLE question_options
    DROP COLUMN noise_sensitivity,
    DROP COLUMN visual_sensitivity,
    DROP COLUMN mobility_preference;
