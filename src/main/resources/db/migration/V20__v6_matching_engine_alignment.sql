-- =====================================================================
-- V20__v6_matching_engine_alignment.sql
-- Schema changes for Kidloop Oneri Motoru v6 (21 Agustos 2026).
--
-- SCOPE OF THIS FILE
--   Schema + parameter-table changes only, generated from a read-only diff
--   of v6.md against the CURRENT migration history (V1-V18) and the CURRENT
--   Java matching engine (ActivityEligibilityPolicy, ActivityScorer,
--   DailyPortfolioBuilder, MatchingStateInitializer, ActivityMatchingService,
--   Child/ParentProfile/ChildDomainLevel/DunnProfile entities). No Java files
--   were changed. No repository state was pushed.
--
-- FILE ORDER REQUIREMENT
--   This migration DROPS activities.is_scaffolded. The activity-pool file
--   Gulcin shared (ids 163-243, uploaded to you as "V18__kidloop_activity_
--   pool_163_243.sql") still INSERTs a value into is_scaffolded, and V18 is
--   already taken in this repo by V18__kidloop_test_activities_calm_pool.sql
--   (ids 129-162). So:
--     1) Rename Gulcin's file to  V19__kidloop_activity_pool_163_243.sql
--        and run it FIRST, unmodified (is_scaffolded still exists then).
--     2) Run this file as V20 AFTER it.
--   If you would rather run this file first, strip the is_scaffolded column
--   (and its TRUE literal) from every INSERT in Gulcin's file before running
--   it as V20; do not run it unmodified after this migration.
--
-- WHY SEVERAL v6 SCHEMA ITEMS ARE MISSING BELOW
--   v6 section 1 assumes a schema state that does not match what's actually
--   on main. Three of its items are already satisfied and need no change:
--     - activities.target_domain: already the 7-value CHECK (done in V9).
--     - activities.difficulty / child_domain_levels.level: already 1-4
--       (done in V14, under the real constraint names chk_activities_
--       difficulty / chk_child_domain_levels_level, not chk_difficulty).
--     - child_domain_levels.domain: already the 7-value CHECK (done in V13).
--   v6's own dunn_profiles snippet also names columns (w_noise, tol_noise,
--   physical_weight) that don't exist; the real table (from V11) uses
--   noise_weight/visual_weight/movement_weight and noise_tolerance/
--   visual_tolerance/movement_tolerance. This file uses the real names.
--
-- BACKWARD COMPATIBILITY WITH THE RUNNING APP (hibernate ddl-auto=validate)
--   ActivityEligibilityPolicy / ActivityScorer / MatchingStateInitializer /
--   ActivityMatchingService currently read scoring_parameters by exact key,
--   with no fallback (Map::get then unbox -> NPE if a key is missing). This
--   migration therefore only ADDS new parameter rows; it never deletes or
--   renames a key the current code reads (freshness_penalty, freshness_
--   lookback_days, attachment_multiplier, domain_initial_level, domain_
--   level_up_streak_threshold, domain_level_down_easier_threshold, harder_/
--   normal_/easier_variation_streak_increment all stay, unused once the v6
--   code lands, until a follow-up migration removes them). Same reasoning
--   for parent_profiles.daily_time_budget_minutes: still read by
--   ActivityMatchingService.budget(), so it is left in place; only the new
--   child-level columns are added here.
--
--   ONE CHANGE HERE IS NOT SAFE TO DEPLOY ALONE:
--   child_domain_levels.streak moves from SMALLINT to NUMERIC(4,1) (section
--   3 below), because v6's credit model is decimal and can go negative.
--   ChildDomainLevel.java currently declares `private short streak;` -- with
--   ddl-auto=validate the app will FAIL TO START against this migration
--   until that field is changed to `BigDecimal`. This is the one item in
--   this file that must not be deployed without its matching Java change.
--
-- WHAT STILL NEEDS CODE (not in this file, listed here so nothing gets lost)
--   - ActivityEligibilityPolicy: C4 hard filter must drop physical_intensity
--     from the max() (v6 A2) -- schema/data already support it either way.
--   - ActivityScorer: remove the T term from the raw-score formula; add the
--     L=4 ceiling sweet-spot rule (harder_variation); switch attachment
--     bonus to BIRLIKTE-only (drop the current "!= BAGIMSIZ" shortcut).
--   - DailyPortfolioBuilder: replace total-score maximization with ordered
--     slot-fill + reserve + the deterministic seeded tie-break (v6 4.2-4.3);
--     replace `Random` with the seed formula in scoring_parameters
--     (tiebreak_seed_a/b/mod); add the GOZETIMLI guarantee pass (4.4) and
--     the 5-level fallback ladder (4.5, writes daily_plans.fallback_level).
--   - ActivityMatchingService.budget(): read children.daily_time_budget_min/
--     max instead of parent_profiles.daily_time_budget_minutes; plan-once-
--     persist semantics (4.7) already hold today since `today()` short-
--     circuits on an existing DailyPlan for the date -- no change needed.
--   - MatchingStateInitializer: age-banded L0 (EK6) using the three new
--     domain_initial_level_* parameters below instead of the flat one.
--   - Onboarding: new three-option range budget question (5.1), replacing
--     the parent_profiles single-value question; new closing screen (5.3).
--   - EK8 publish validation (7.1) is a service-layer gate, not a
--     constraint -- deliberately not modeled as a CHECK here.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1) activities -- drop is_scaffolded (v6 section 1.1)
--    Not mapped by Activity.java (checked): dropping it is safe for
--    hibernate ddl-auto=validate. Support is easier_variation-only now,
--    exactly as V8/V10 already treat it in the seed data.
--
--    v_scorable_activities (V1) SELECTs is_scaffolded, so Postgres refuses
--    the DROP COLUMN until that dependency is gone. The view is dead: it's
--    the only view in the schema, defined once in V1 and never queried by
--    any repository, service, or later migration -- the matching engine
--    reads activities directly (ActivityRepository.findEligibleBasePool).
--    Dropping it outright rather than recreating it without the column.
-- ---------------------------------------------------------------------
DROP VIEW v_scorable_activities;

ALTER TABLE activities DROP COLUMN is_scaffolded;


-- ---------------------------------------------------------------------
-- 2) dunn_profiles -- C4 gets real distance weights (v6 section 1.2, 2.1)
--    chk_dunn_profile_weight_mode (V11) forces C4's three weight columns
--    to stay NULL; it must go before C4 can get real weights. Movement
--    axis moving from hard-filter to D-penalty (v6 A2) is what makes this
--    necessary -- v5 never scored D for C4, v6 does.
-- ---------------------------------------------------------------------
ALTER TABLE dunn_profiles DROP CONSTRAINT chk_dunn_profile_weight_mode;

UPDATE dunn_profiles
SET noise_weight = 5.00, visual_weight = 5.00, movement_weight = 3.00
WHERE quadrant = 'C4';

-- ---------------------------------------------------------------------
-- 3) developmental_period_tasks -- last band's upper bound 72 -> 73
--    (v6 section 1.3, EK1)
--    This is a live bug fix, not just future-proofing: Developmental
--    PeriodTaskRepository.findForAge uses maxAgeMonths > :age (strict),
--    so a child of EXACTLY 72 months currently matches no band at all and
--    ActivityMatchingService.today() throws IllegalStateException
--    ("Developmental period is missing") for that child today.
-- ---------------------------------------------------------------------
UPDATE developmental_period_tasks
SET max_age_months = 73
WHERE min_age_months = 48 AND max_age_months = 72 AND target_domain = 'SOCIAL_EMOTIONAL';


-- ---------------------------------------------------------------------
-- 4) children -- per-child budget range (v6 section 1.4, 5.1)
--    Added additively. parent_profiles.daily_time_budget_minutes is left
--    in place on purpose: ActivityMatchingService.budget() still reads it
--    and dropping it here would break every plan request until that
--    method is repointed at the child-level columns.
--
--    Backfill: v6 says existing single-value answers map to their nearest
--    range and children get option B (25-35) as the default. We only have
--    a value to map from at the PARENT (household) level today, so:
--      parent 10  -> (15,25)   parent 20 -> (25,35)   parent 30 -> (35,45)
--      parent NULL / no profile -> (25,35), matching v6's own default.
-- ---------------------------------------------------------------------
ALTER TABLE children
    ADD COLUMN daily_time_budget_min SMALLINT,
    ADD COLUMN daily_time_budget_max SMALLINT;

UPDATE children c
SET daily_time_budget_min = CASE pp.daily_time_budget_minutes
                                 WHEN 10 THEN 15
                                 WHEN 20 THEN 25
                                 WHEN 30 THEN 35
                                 ELSE 25
                             END,
    daily_time_budget_max = CASE pp.daily_time_budget_minutes
                                 WHEN 10 THEN 25
                                 WHEN 20 THEN 35
                                 WHEN 30 THEN 45
                                 ELSE 35
                             END
FROM parent_profiles pp
WHERE pp.id = c.parent_id;

-- Any child whose parent row was missing/deleted at backfill time: default.
UPDATE children
SET daily_time_budget_min = 25, daily_time_budget_max = 35
WHERE daily_time_budget_min IS NULL;

ALTER TABLE children
    ALTER COLUMN daily_time_budget_min SET NOT NULL,
    ALTER COLUMN daily_time_budget_max SET NOT NULL;

ALTER TABLE children
    ADD CONSTRAINT chk_children_time_budget_range
        CHECK ((daily_time_budget_min, daily_time_budget_max) IN ((15, 25), (25, 35), (35, 45)));


-- ---------------------------------------------------------------------
-- 5) child_domain_levels -- decimal, signed streak (v6 section 1.5, 3.2)
--    BREAKING for the current entity -- see the file header. level stays
--    untouched: chk_child_domain_levels_level already enforces 1-4 (V14),
--    matching v6's L in [1,4] with no further change needed.
-- ---------------------------------------------------------------------
ALTER TABLE child_domain_levels
    DROP CONSTRAINT child_domain_levels_streak_check;

ALTER TABLE child_domain_levels
    ALTER COLUMN streak TYPE NUMERIC(4,1),
    ALTER COLUMN streak SET DEFAULT 0;


-- ---------------------------------------------------------------------
-- 6) daily_plans -- plan-level bookkeeping (v6 section 1.6)
--    Defaults are kept permanently (not dropped after backfill): unlike
--    the children budget migration above, there is no historically
--    correct value to converge existing rows to, so the default simply
--    covers rows written by the current DailyPlan(childId, date)
--    constructor until DailyPortfolioBuilder is updated to set them.
-- ---------------------------------------------------------------------
ALTER TABLE daily_plans
    ADD COLUMN budget_min                  INT      NOT NULL DEFAULT 25,
    ADD COLUMN budget_max                  INT      NOT NULL DEFAULT 35,
    ADD COLUMN committed_duration_minutes  INT      NOT NULL DEFAULT 0,
    ADD COLUMN total_duration_minutes      INT      NOT NULL DEFAULT 0,
    ADD COLUMN fallback_level              SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE daily_plans
    ADD CONSTRAINT chk_daily_plans_fallback_level CHECK (fallback_level BETWEEN 0 AND 4);


-- ---------------------------------------------------------------------
-- 7) daily_plan_items -- per-item display flags (v6 section 1.6)
-- ---------------------------------------------------------------------
ALTER TABLE daily_plan_items
    ADD COLUMN within_budget  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN repeat_notice  BOOLEAN NOT NULL DEFAULT FALSE;


-- ---------------------------------------------------------------------
-- 8) scoring_parameters -- new v6 keys, additive only (v6 section 1.7)
--    Booleans are stored as 1.00/0.00 (this table has no boolean column;
--    that convention is documented per-row below).
--
--    Where a v6 key is the same concept as an existing one, BOTH are kept
--    rather than renaming in place, so the current code (which reads the
--    old key) keeps working: attachment_multiplier (existing) vs.
--    attachment_multiplier_together (new, same value); high_separation_
--    anxiety_threshold (existing, also drives the BAGIMSIZ hard filter)
--    vs. attachment_anxiety_threshold (new, attachment-only name);
--    domain_level_up_streak_threshold (existing) vs. level_up_threshold
--    (new). A follow-up migration can drop the old ones once the v6 Java
--    changes land and stop reading them.
--
--    scoring_parameters.value is NUMERIC(12,4) (V11) -- 8 integer digits,
--    max ~99,999,999. tiebreak_seed_mod is 2147483647 (2^31-1, 10 digits)
--    by design (v6 EK4: "keeps it stable across 32-bit implementations"),
--    so the column is widened rather than shrinking the constant.
--    ScoringParameter.java maps this to a plain BigDecimal with no
--    @Column(precision=...): Hibernate validate doesn't check precision/
--    scale for that mapping, so widening is safe with the app unchanged.
-- ---------------------------------------------------------------------
ALTER TABLE scoring_parameters ALTER COLUMN value TYPE NUMERIC(20,4);

INSERT INTO scoring_parameters (parameter_key, value, description) VALUES
    -- Deterministic tie-break (v6 4.3, EK4) -- replaces DailyPortfolioBuilder's
    -- java.util.Random, which currently makes tie-broken slots non-reproducible
    -- (can differ between two requests for the same child on the same day).
    ('tiebreak_seed_a',                       1000003,  'Prime multiplier for childId in the deterministic tie-break seed'),
    ('tiebreak_seed_b',                         10007,  'Prime multiplier for the plan-date integer (YYYYMMDD) in the tie-break seed'),
    ('tiebreak_seed_mod',                  2147483647,  'Modulus for the tie-break seed (2^31 - 1), keeps it stable across 32-bit implementations'),

    -- Dynamic freshness window as a pool-elimination filter, replacing the
    -- flat T penalty. freshness_penalty / freshness_lookback_days (existing)
    -- are left in place until ActivityScorer/ActivityMatchingService switch
    -- to this filter and drop the T term entirely.
    ('freshness_window_divisor',                    6,  'Divides pool size to size the recent-plans exclusion window (v6 3.4 step 7)'),
    ('freshness_window_min',                        2,  'Minimum plans considered recent regardless of pool size'),

    -- Attachment: multiplier narrows to BIRLIKTE only; GOZETIMLI moves to a
    -- guarantee rule (v6 2.5, 4.4) instead of a multiplier.
    ('attachment_multiplier_together',            1.15,  'B multiplier for BIRLIKTE activities when separation anxiety is high (v6: GOZETIMLI no longer multiplies)'),
    ('attachment_multiplier_supervised',          1.00,  'B multiplier for GOZETIMLI activities under v6 -- explicitly 1.00; visibility comes from the guarantee rule, not a multiplier'),
    ('attachment_anxiety_threshold',              4.00,  'Separation-anxiety score at/above which the attachment rules apply (same value as high_separation_anxiety_threshold, kept as a separate key for the attachment-specific rules)'),
    ('attachment_exclude_independent',            1.00,  'Boolean (1=true/0=false): BAGIMSIZ activities stay excluded when anxious'),
    ('attachment_guarantee_supervised',           1.00,  'Boolean (1=true/0=false): if no GOZETIMLI activity made the plan, swap one into EXPLORE (v6 4.4)'),

    -- Level ceiling (v6 3.1, B8)
    ('level_max',                                 4.00,  'Upper bound for child_domain_levels.level (already enforced by chk_child_domain_levels_level)'),
    ('level_min',                                 1.00,  'Lower bound for child_domain_levels.level (already enforced by chk_child_domain_levels_level)'),

    -- Difficulty-position-based streak credit (v6 3.2, B3), replacing the
    -- played-variation-based increments (harder_/normal_/easier_variation_
    -- streak_increment, existing, left in place -- unused once this lands,
    -- since v6 explicitly defers played_variation entering the UI, section 10).
    ('level_up_threshold',                        3.00,  'Streak value at/above which level increases by 1 and streak resets to 0'),
    ('level_down_threshold',                     -1.00,  'Streak value below which level decreases by 1 (floor level_min) and streak resets to 0'),
    ('level_credit_stretch',                       1.00,  'Streak credit on LIKED when difficulty >= level + 1'),
    ('level_credit_at_level',                      0.50,  'Streak credit on LIKED when difficulty = level'),
    ('level_credit_below',                         0.00,  'Streak credit on LIKED when difficulty < level'),
    ('level_penalty_struggle_stretch',            -0.50,  'Streak penalty on STRUGGLED when difficulty = level + 1'),
    ('level_penalty_struggle_at_level',           -1.00,  'Streak penalty on STRUGGLED when difficulty <= level (difficulty > level + 1 costs nothing)'),
    ('ceiling_counter_cap',                        1.00,  'Streak cannot accumulate above this at level_max, so a single down-vote can still trigger a level-down'),
    ('ceiling_sweet_spot_requires_harder',         1.00,  'Boolean (1=true/0=false): at level_max, the sweet-spot bonus requires harder_variation instead of easier_variation'),

    -- Age-banded initial level (v6 3.1, EK6), replacing the flat
    -- domain_initial_level (existing, left in place -- MatchingStateInitializer
    -- still reads it for every new child regardless of age until updated).
    -- NOTE: these age cuts (48/60/73 months) are independent of the
    -- questionnaire AgeBand enum (12/24/48/72 months) -- different concept,
    -- do not conflate the two when this lands in Java.
    ('domain_initial_level_under_48m',            1.00,  'Initial child_domain_levels.level for age < 48 months'),
    ('domain_initial_level_48_to_60m',             2.00,  'Initial child_domain_levels.level for 48 <= age < 60 months'),
    ('domain_initial_level_60_to_73m',             3.00,  'Initial child_domain_levels.level for 60 <= age <= 72 months');

COMMIT;
