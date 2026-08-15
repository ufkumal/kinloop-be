-- =============================================================================
-- Matching-engine configuration (Kidloop recommendation engine v5).
--
-- These tables are the single source of truth for Dunn profiles,
-- developmental-period tasks, and every numeric scoring/learning parameter.
-- Application code must read these values rather than duplicating constants.
-- =============================================================================


-- Five canonical Dunn profiles. C4 deliberately has no distance weights:
-- activities with a load at or above the configured hard-filter threshold are
-- removed before scoring, as required by the v5 filtering rules.
CREATE TABLE dunn_profiles (
    quadrant                  VARCHAR(10) PRIMARY KEY
        CHECK (quadrant IN ('C1', 'C2', 'C3', 'C4', 'MIXED')),
    display_name              VARCHAR(100) NOT NULL,
    noise_tolerance           SMALLINT     NOT NULL CHECK (noise_tolerance BETWEEN 1 AND 5),
    visual_tolerance          SMALLINT     NOT NULL CHECK (visual_tolerance BETWEEN 1 AND 5),
    movement_tolerance        SMALLINT     NOT NULL CHECK (movement_tolerance BETWEEN 1 AND 5),
    noise_weight              NUMERIC(5,2)          CHECK (noise_weight > 0),
    visual_weight             NUMERIC(5,2)          CHECK (visual_weight > 0),
    movement_weight           NUMERIC(5,2)          CHECK (movement_weight > 0),
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_dunn_profile_weight_mode CHECK (
        (quadrant = 'C4' AND noise_weight IS NULL AND visual_weight IS NULL AND movement_weight IS NULL)
        OR
        (quadrant <> 'C4' AND noise_weight IS NOT NULL AND visual_weight IS NOT NULL AND movement_weight IS NOT NULL)
    )
);

CREATE TRIGGER trg_dunn_profiles_updated_at
    BEFORE UPDATE ON dunn_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO dunn_profiles (
    quadrant, display_name,
    noise_tolerance, visual_tolerance, movement_tolerance,
    noise_weight, visual_weight, movement_weight
) VALUES
    ('C1',    'Sakin gözlemci',    3, 3, 3,  5.00,  5.00, 3.00),
    ('C2',    'Enerjik kaşif',     4, 4, 5,  3.00,  3.00, 6.00),
    ('C3',    'Hassas izleyici',   2, 2, 3, 10.00, 10.00, 6.00),
    ('C4',    'Korunmacı',         1, 2, 2,  NULL,   NULL, NULL),
    ('MIXED', 'Henüz belirsiz',    3, 3, 3,  5.00,  5.00, 3.00);


-- Half-open age bands: min_age_months <= age < max_age_months.
CREATE TABLE developmental_period_tasks (
    id                 BIGSERIAL PRIMARY KEY,
    min_age_months     SMALLINT    NOT NULL CHECK (min_age_months >= 0),
    max_age_months     SMALLINT    NOT NULL CHECK (max_age_months > min_age_months),
    target_domain      VARCHAR(30) NOT NULL
        CHECK (target_domain IN ('GROSS_MOTOR', 'FINE_MOTOR', 'LANGUAGE',
            'SOCIAL_EMOTIONAL', 'COGNITIVE', 'SENSORY', 'SELF_REGULATION')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_developmental_period_age_band UNIQUE (min_age_months, max_age_months)
);

CREATE INDEX idx_developmental_period_age_range
    ON developmental_period_tasks (min_age_months, max_age_months);

CREATE TRIGGER trg_developmental_period_tasks_updated_at
    BEFORE UPDATE ON developmental_period_tasks
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO developmental_period_tasks (min_age_months, max_age_months, target_domain) VALUES
    (0,  12, 'GROSS_MOTOR'),
    (12, 24, 'GROSS_MOTOR'),
    (24, 48, 'LANGUAGE'),
    (48, 72, 'SOCIAL_EMOTIONAL');


-- Numeric engine parameters use stable keys so values can be calibrated
-- without a deployment. The description records the v5 rule each value serves.
CREATE TABLE scoring_parameters (
    parameter_key VARCHAR(100) PRIMARY KEY,
    value         NUMERIC(12,4) NOT NULL,
    description   TEXT          NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_scoring_parameters_updated_at
    BEFORE UPDATE ON scoring_parameters
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO scoring_parameters (parameter_key, value, description) VALUES
    -- Profile initialization and onboarding priors
    ('gardner_initial_score',                 3.00, 'Initial score for each of the eight Gardner intelligences'),
    ('gardner_prior_min_score',               2.00, 'Lower clamp after onboarding priors are applied'),
    ('gardner_prior_max_score',               4.00, 'Upper clamp after onboarding priors are applied'),
    ('gardner_runtime_min_score',             0.00, 'Absolute lower bound after feedback'),
    ('gardner_runtime_max_score',             5.00, 'Absolute upper bound after feedback'),
    ('domain_initial_level',                  1.00, 'Initial level for each developmental domain'),

    -- Hard filters
    ('c4_hard_filter_load_threshold',         3.00, 'C4 activities are excluded when any sensory load is at least this value'),
    ('high_separation_anxiety_threshold',     4.00, 'Independent activities are excluded and attachment multiplier becomes eligible'),
    ('short_focus_max_duration_minutes',     10.00, 'Maximum activity duration when focus band is SHORT'),

    -- Core score: (base - D + G + P + Z) * B - T
    ('score_base',                           100.00, 'Base score for every activity that passes hard filters'),
    ('score_display_min',                      0.00, 'Minimum parent-facing clamped score'),
    ('score_display_max',                    100.00, 'Maximum parent-facing clamped score'),
    ('developmental_period_bonus',            15.00, 'P bonus when activity domain matches the age-period task'),
    ('zpd_sweet_spot_bonus',                  20.00, 'Z bonus for difficulty L+1 with an easier variation'),
    ('zpd_frustration_penalty',              -25.00, 'Z contribution when difficulty is above L+1'),
    ('zpd_boredom_penalty',                   -5.00, 'Z contribution when difficulty is below L'),
    ('gardner_comfort_threshold',              4.00, 'Target-intelligence score that opens the comfort gate'),
    ('gardner_comfort_bonus',                 10.00, 'G bonus for a target intelligence at the comfort threshold'),
    ('gardner_bridge_target_threshold',        2.50, 'Maximum weak target score eligible for a bridge'),
    ('gardner_bridge_secondary_threshold',     4.00, 'Minimum secondary-intelligence score required for a bridge'),
    ('gardner_bridge_bonus',                  15.00, 'G bonus for reaching a weak target through a strong secondary intelligence'),
    ('gardner_block_threshold',                1.50, 'Maximum target score at which an unbridged activity is discouraged'),
    ('gardner_block_penalty',                -15.00, 'G contribution for a strongly disliked target without a strong bridge'),
    ('attachment_multiplier',                  1.15, 'B multiplier for high-anxiety children in together or supervised activities'),
    ('freshness_penalty',                     30.00, 'T penalty when the activity appeared in the previous day plan'),
    ('freshness_lookback_days',                1.00, 'Number of preceding plan days considered for freshness'),

    -- Plan construction
    ('slot_candidate_limit',                   5.00, 'Maximum candidates retained per slot before combination search'),
    ('explore_random_top_limit',               3.00, 'Top candidates among which the exploration tie is randomized'),

    -- Feedback learning
    ('liked_target_delta',                     0.30, 'Gardner delta for the target intelligence after LIKED'),
    ('liked_secondary_delta',                  0.15, 'Gardner delta for the secondary intelligence after LIKED'),
    ('disliked_interest_delta',               -0.15, 'Gardner delta after DISLIKED resolves to INTEREST'),
    ('harder_variation_streak_increment',      2.00, 'Domain streak increment after successful HARDER play'),
    ('normal_variation_streak_increment',      1.00, 'Domain streak increment after successful NORMAL or unspecified play'),
    ('easier_variation_streak_increment',      0.00, 'Domain streak increment after successful EASIER play'),
    ('domain_level_up_streak_threshold',       3.00, 'Accumulated success streak required to increase a domain level'),
    ('domain_level_down_easier_threshold',     3.00, 'Consecutive EASIER plays required to decrease a domain level'),
    ('feedback_max_votes_per_day',             3.00, 'Maximum accepted home-plan votes per child per day'),
    ('feedback_daily_domain_increase_cap',     0.90, 'Maximum Gardner increase for one intelligence in one day'),
    ('feedback_signal_decay_age_days',        90.00, 'Age after which historical signals receive reduced weight'),
    ('feedback_signal_decay_multiplier',       0.50, 'Weight multiplier for signals older than the decay age'),

    -- Free-text interpretation safeguards (phase 2)
    ('llm_signal_max_absolute_delta',          0.30, 'Maximum absolute delta proposed by one free-text signal'),
    ('llm_signal_confidence_threshold',        0.60, 'Minimum confidence required to apply a free-text signal'),

    -- MIXED resolution and preference prompt
    ('mixed_resolution_feedback_threshold',   10.00, 'Feedback count that triggers MIXED quadrant resolution'),
    ('mixed_resolution_age_days',             30.00, 'Days after profile creation that trigger MIXED resolution'),
    ('mixed_resolution_direction_threshold',   3.00, 'Directional evidence required to derive a non-default quadrant'),
    ('preference_prompt_feedback_threshold',   5.00, 'Feedback count that can trigger the comfort/development preference prompt'),
    ('preference_prompt_score_threshold',      3.90, 'Maximum Gardner score that can trigger the preference prompt');
