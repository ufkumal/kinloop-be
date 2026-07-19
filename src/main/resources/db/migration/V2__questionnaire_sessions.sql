-- =============================================================================
-- V2__questionnaire_sessions.sql
-- Kinloop - age-banded questionnaire + versioned child profile
--
-- WHAT CHANGES vs V1
--   V1 assumption: the algorithm reads structural columns on `children`, and
--   `child_answers` is a flat archive. That cannot express "the child grew and
--   the answers changed" - it only ever holds one current state.
--
--   V2 replaces it with:
--     questionnaire_sessions   NEW  - one pass through the question set for
--                                     one age band. Immutable once COMPLETED.
--     child_answers            ALTER- gains session_id. SOURCE OF TRUTH.
--     child_profile_snapshots  NEW  - derived profile per completed session.
--                                     A CACHE. Rebuildable from child_answers.
--                                     Written ONLY by ProfileSnapshotService.
--     children                 ALTER- structural columns dropped; current
--                                     profile now lives in the snapshot marked
--                                     is_current.
--     questions/question_options ALTER - scope, required, DATE type, and the
--                                     answer -> value mapping as typed columns.
--
-- HISTORY GUARANTEE
--   Crossing an age band never updates an existing row. A new session opens, a
--   new snapshot is written, the previous snapshot flips is_current = FALSE and
--   stays queryable forever. That is the improvement signal.
--
-- AGE BAND RESOLUTION (implement exactly this in AgeBand.resolve)
--   min_age_months <= age_months < max_age_months
--   except BAND_48_72, where age_months <= 72 is inclusive.
--
-- PRECONDITION
--   Assumes child_answers is empty (no production data yet). If it is not,
--   backfill session_id before the NOT NULL is applied.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. questions - add scope + required, allow DATE answers (Q1 is a birth date)
-- -----------------------------------------------------------------------------
ALTER TABLE questions
    ADD COLUMN scope    VARCHAR(16) NOT NULL DEFAULT 'CHILD',
    ADD COLUMN required BOOLEAN     NOT NULL DEFAULT TRUE;

ALTER TABLE questions
    ADD CONSTRAINT chk_questions_scope CHECK (scope IN ('CHILD', 'HOUSEHOLD'));

-- V1 declared question_type inline, so Postgres named it questions_question_type_check.
ALTER TABLE questions DROP CONSTRAINT IF EXISTS questions_question_type_check;
ALTER TABLE questions
    ADD CONSTRAINT chk_questions_type
        CHECK (question_type IN ('SINGLE_CHOICE','MULTI_CHOICE','SCALE','FREE_TEXT','DATE'));

COMMENT ON COLUMN questions.scope IS
    'HOUSEHOLD questions (Q7) are answered once per parent and land on parent_profiles.';
COMMENT ON COLUMN questions.required IS
    'A session cannot be COMPLETED while a required question for its band is unanswered.';


-- -----------------------------------------------------------------------------
-- 2. question_options - the answer -> value mapping, as typed columns.
--    Gulcin retunes this table; no deploy, and history recomputes.
--    scoring_hint is dropped: two mapping mechanisms means two sources of truth.
-- -----------------------------------------------------------------------------
ALTER TABLE question_options DROP COLUMN scoring_hint;

ALTER TABLE question_options
    ADD COLUMN dunn_quadrant             VARCHAR(10),
    ADD COLUMN noise_sensitivity         SMALLINT,
    ADD COLUMN visual_sensitivity        SMALLINT,
    ADD COLUMN mobility_preference       SMALLINT,
    ADD COLUMN separation_anxiety        SMALLINT,
    ADD COLUMN social_orientation        VARCHAR(20),
    ADD COLUMN focus_band                VARCHAR(10),
    ADD COLUMN daily_time_budget_minutes SMALLINT,
    -- multi-valued: Q5 option D sets two priors at once. Domains MUST match
    -- child_intelligence_scores.intelligence_type.
    -- [{"domain":"INTRAPERSONAL","delta":0.5},{"domain":"INTERPERSONAL","delta":-0.5}]
    ADD COLUMN gardner_priors            JSONB;

ALTER TABLE question_options
    ADD CONSTRAINT chk_qo_dunn        CHECK (dunn_quadrant IS NULL OR dunn_quadrant IN ('C1','C2','C3','C4','MIXED')),
    ADD CONSTRAINT chk_qo_noise       CHECK (noise_sensitivity   IS NULL OR noise_sensitivity   BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_qo_visual      CHECK (visual_sensitivity  IS NULL OR visual_sensitivity  BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_qo_mobility    CHECK (mobility_preference IS NULL OR mobility_preference BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_qo_separation  CHECK (separation_anxiety  IS NULL OR separation_anxiety  BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_qo_social      CHECK (social_orientation  IS NULL OR social_orientation  IN ('LEADER','COOPERATIVE','OBSERVER','PARALLEL','SOLITARY')),
    ADD CONSTRAINT chk_qo_focus       CHECK (focus_band          IS NULL OR focus_band          IN ('SHORT','MEDIUM','LONG')),
    ADD CONSTRAINT chk_qo_time_budget CHECK (daily_time_budget_minutes IS NULL OR daily_time_budget_minutes IN (10,20,30));


-- -----------------------------------------------------------------------------
-- 3. questionnaire_sessions (NEW)
-- -----------------------------------------------------------------------------
CREATE TABLE questionnaire_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    child_id            BIGINT      NOT NULL
        CONSTRAINT fk_qs_child REFERENCES children (id) ON DELETE CASCADE,
    age_band            VARCHAR(16) NOT NULL,
    age_months_at_start SMALLINT    NOT NULL CHECK (age_months_at_start >= 0),
    trigger_reason      VARCHAR(16) NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_qs_age_band  CHECK (age_band IN ('BAND_0_12','BAND_12_24','BAND_24_48','BAND_48_72')),
    CONSTRAINT chk_qs_trigger   CHECK (trigger_reason IN ('INITIAL','AGE_BAND','MANUAL')),
    CONSTRAINT chk_qs_status    CHECK (status IN ('IN_PROGRESS','COMPLETED','ABANDONED')),
    CONSTRAINT chk_qs_completed CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL))
);

-- At most one open session per child: the resume target on next login.
CREATE UNIQUE INDEX uq_qs_open_per_child
    ON questionnaire_sessions (child_id) WHERE status = 'IN_PROGRESS';

CREATE INDEX idx_qs_child_completed
    ON questionnaire_sessions (child_id, completed_at DESC) WHERE status = 'COMPLETED';

CREATE TRIGGER trg_qs_updated_at
    BEFORE UPDATE ON questionnaire_sessions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- -----------------------------------------------------------------------------
-- 4. child_answers - now scoped to a session. SOURCE OF TRUTH.
--    Updatable while its session is IN_PROGRESS (parent changes their mind
--    mid-flow); frozen once COMPLETED - enforced in the service layer, not
--    here, so the snapshot rebuild job can still read them.
-- -----------------------------------------------------------------------------
ALTER TABLE child_answers
    ADD COLUMN session_id BIGINT NOT NULL
        CONSTRAINT fk_child_answers_session
            REFERENCES questionnaire_sessions (id) ON DELETE CASCADE,
    ADD COLUMN date_value DATE;

-- V1's uniqueness was (child, question, option) - that forbids ever re-answering
-- a question in a later age band, which is exactly what we now need to allow.
ALTER TABLE child_answers DROP CONSTRAINT uq_child_answers;
ALTER TABLE child_answers
    ADD CONSTRAINT uq_child_answers_session_question UNIQUE (session_id, question_id);

-- exactly one answer form must be present
ALTER TABLE child_answers
    ADD CONSTRAINT chk_child_answers_answer_form CHECK (
        (question_option_id IS NOT NULL)::int
      + (numeric_value      IS NOT NULL)::int
      + (text_value         IS NOT NULL)::int
      + (date_value         IS NOT NULL)::int = 1
    );

CREATE INDEX idx_child_answers_session ON child_answers (session_id);

COMMENT ON COLUMN child_answers.child_id IS
    'Denormalised from the session for query convenience. Service layer must
     guarantee child_answers.child_id = questionnaire_sessions.child_id.';


-- -----------------------------------------------------------------------------
-- 5. child_profile_snapshots (NEW) - DERIVED CACHE
--
--    One row per completed session. Holds the fold of ALL completed sessions
--    for that child up to that point (latest non-null wins), not just its own
--    session's answers - so if a question is ever removed from a later band the
--    last known value carries forward instead of silently becoming null.
--
--    This is the single row the recommendation algorithm reads.
--    NEVER hand-edited. Write path is ProfileSnapshotService.rebuild() only.
-- -----------------------------------------------------------------------------
CREATE TABLE child_profile_snapshots (
    id                    BIGSERIAL PRIMARY KEY,
    child_id              BIGINT      NOT NULL
        CONSTRAINT fk_cps_child REFERENCES children (id) ON DELETE CASCADE,
    session_id            BIGINT      NOT NULL
        CONSTRAINT fk_cps_session REFERENCES questionnaire_sessions (id) ON DELETE CASCADE,
    age_band              VARCHAR(16) NOT NULL,
    age_months_at_capture SMALLINT    NOT NULL CHECK (age_months_at_capture >= 0),

    dunn_quadrant         VARCHAR(10),
    noise_sensitivity     SMALLINT,
    visual_sensitivity    SMALLINT,
    mobility_preference   SMALLINT,
    separation_anxiety    SMALLINT,
    social_orientation    VARCHAR(20),
    focus_band            VARCHAR(10),
    gardner_priors        JSONB,

    is_current            BOOLEAN     NOT NULL DEFAULT TRUE,
    captured_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_cps_session    UNIQUE (session_id),
    CONSTRAINT chk_cps_age_band  CHECK (age_band IN ('BAND_0_12','BAND_12_24','BAND_24_48','BAND_48_72')),
    CONSTRAINT chk_cps_dunn      CHECK (dunn_quadrant IS NULL OR dunn_quadrant IN ('C1','C2','C3','C4','MIXED')),
    CONSTRAINT chk_cps_noise     CHECK (noise_sensitivity   IS NULL OR noise_sensitivity   BETWEEN 1 AND 5),
    CONSTRAINT chk_cps_visual    CHECK (visual_sensitivity  IS NULL OR visual_sensitivity  BETWEEN 1 AND 5),
    CONSTRAINT chk_cps_mobility  CHECK (mobility_preference IS NULL OR mobility_preference BETWEEN 1 AND 5),
    CONSTRAINT chk_cps_separation CHECK (separation_anxiety IS NULL OR separation_anxiety  BETWEEN 1 AND 5),
    CONSTRAINT chk_cps_social    CHECK (social_orientation  IS NULL OR social_orientation  IN ('LEADER','COOPERATIVE','OBSERVER','PARALLEL','SOLITARY')),
    CONSTRAINT chk_cps_focus     CHECK (focus_band          IS NULL OR focus_band          IN ('SHORT','MEDIUM','LONG'))
);

-- Exactly one current profile per child.
CREATE UNIQUE INDEX uq_cps_current_per_child
    ON child_profile_snapshots (child_id) WHERE is_current;

-- History reads, newest first.
CREATE INDEX idx_cps_child_captured
    ON child_profile_snapshots (child_id, captured_at DESC);

CREATE TRIGGER trg_cps_updated_at
    BEFORE UPDATE ON child_profile_snapshots
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE child_profile_snapshots IS
    'Derived cache of child_answers. Rebuildable. Write only via ProfileSnapshotService.';


-- -----------------------------------------------------------------------------
-- 6. children - retire the structural columns.
--    They were the single current state; that role now belongs to the snapshot
--    marked is_current. Leaving them would give us two sources of truth.
--    preference_mode and onboarding_completed_at stay: neither is age-banded.
-- -----------------------------------------------------------------------------
ALTER TABLE children
    DROP COLUMN dunn_quadrant,
    DROP COLUMN noise_sensitivity,
    DROP COLUMN visual_sensitivity,
    DROP COLUMN mobility_preference,
    DROP COLUMN separation_anxiety,
    DROP COLUMN social_orientation,
    DROP COLUMN focus_band,
    DROP COLUMN daily_time_budget_minutes;


-- -----------------------------------------------------------------------------
-- 7. Q7 is per household, not per child (Matris 1.8: "hane basina bir kez").
-- -----------------------------------------------------------------------------
ALTER TABLE parent_profiles
    ADD COLUMN daily_time_budget_minutes SMALLINT,
    ADD CONSTRAINT chk_parent_profiles_time_budget
        CHECK (daily_time_budget_minutes IS NULL OR daily_time_budget_minutes IN (10,20,30));


-- =============================================================================
-- SEED - Matris 1 (Gulcin, v2)
-- =============================================================================

INSERT INTO questions (code, body, question_type, scope, min_age_months, max_age_months, display_order) VALUES
 ('Q1',  'Cocugunuzun dogum tarihi?',                           'DATE',          'CHILD',      0, 72, 1),
 ('Q2b', 'Yuksek ses/kalabalikta bebeginiz:',                   'SINGLE_CHOICE', 'CHILD',      0, 12, 2),
 ('Q2',  'Kalabalik, muzikli bir ortama girince cocugunuz:',    'SINGLE_CHOICE', 'CHILD',     12, 72, 2),
 ('Q3',  'Gun icinde genellikle:',                              'SINGLE_CHOICE', 'CHILD',     12, 72, 3),
 ('Q4b', 'Baskasinin kucaginda ne kadar rahat?',                'SINGLE_CHOICE', 'CHILD',      0, 12, 4),
 ('Q4',  'Siz yaninda yokken?',                                 'SINGLE_CHOICE', 'CHILD',     12, 48, 4),
 ('Q4c', 'Tanidik yetiskinle sizsiz ortamda?',                  'SINGLE_CHOICE', 'CHILD',     48, 72, 4),
 ('Q5',  'Diger cocuklarla nasil oynar?',                       'SINGLE_CHOICE', 'CHILD',     24, 72, 5),
 ('Q6',  'Bir ise kesintisiz ne kadar odaklanir?',              'SINGLE_CHOICE', 'CHILD',     48, 72, 6),
 ('Q7',  'Cocugunuzla gunde ne kadar vakit ayirabiliyorsunuz?', 'SINGLE_CHOICE', 'HOUSEHOLD',  0, 72, 7);


-- Q2b - bebek duyusal arketipi (0-12 ay)
INSERT INTO question_options (question_id, code, label, display_order, dunn_quadrant, noise_sensitivity, visual_sensitivity, gardner_priors)
SELECT q.id, v.code, v.label, v.ord, v.dq, v.noise, v.visual, v.priors::jsonb
FROM questions q, (VALUES
    ('A', 'Uyur / etkilenmeden devam eder', 1, 'C1', 4::smallint, 4::smallint, NULL),
    ('B', 'Heyecanlanir, canlanir',         2, 'C2', 4::smallint, 4::smallint, '[{"domain":"BODILY_KINAESTHETIC","delta":0.5}]'),
    ('C', 'Huzursuzlanir, aglamakli olur',  3, 'C3', 2::smallint, 2::smallint, NULL),
    ('D', 'Aglar, zor sakinlesir',          4, 'C4', 1::smallint, 2::smallint, NULL)
) AS v(code, label, ord, dq, noise, visual, priors)
WHERE q.code = 'Q2b';


-- Q2 - duyusal arketip (12-72 ay)
INSERT INTO question_options (question_id, code, label, display_order, dunn_quadrant, noise_sensitivity, visual_sensitivity, gardner_priors)
SELECT q.id, v.code, v.label, v.ord, v.dq, v.noise, v.visual, v.priors::jsonb
FROM questions q, (VALUES
    ('A', 'Fark etmez, kendi halinde',     1, 'C1',    4::smallint, 4::smallint, NULL),
    ('B', 'Cosar, ziplar, ortama katilir', 2, 'C2',    4::smallint, 4::smallint, '[{"domain":"BODILY_KINAESTHETIC","delta":0.5}]'),
    ('C', 'Katlanir ama gerginlesir',      3, 'C3',    2::smallint, 2::smallint, NULL),
    ('D', 'Kacar, kulaklarini kapatir',    4, 'C4',    1::smallint, 2::smallint, NULL),
    ('E', 'Ortama gore degisir',           5, 'MIXED', 3::smallint, 3::smallint, NULL)
) AS v(code, label, ord, dq, noise, visual, priors)
WHERE q.code = 'Q2';


-- Q3 - hareket duzeyi (12-72 ay)
INSERT INTO question_options (question_id, code, label, display_order, mobility_preference)
SELECT q.id, v.code, v.label, v.ord, v.mob
FROM questions q, (VALUES
    ('A', 'Surekli hareket halinde',   1, 5::smallint),
    ('B', 'Hareketli ama durabiliyor', 2, 4::smallint),
    ('C', 'Dengeli',                   3, 3::smallint),
    ('D', 'Oturmali oyunlari sever',   4, 2::smallint),
    ('E', 'Hep sakindir',              5, 1::smallint)
) AS v(code, label, ord, mob)
WHERE q.code = 'Q3';


-- Q4 family - ayrilik kaygisi. One scale, three age-specific promptss.
INSERT INTO question_options (question_id, code, label, display_order, separation_anxiety)
SELECT q.id, v.code, v.label, v.ord, v.anx
FROM questions q, (VALUES
    ('1', 'Cok rahat',           1, 1::smallint),
    ('2', 'Cogunlukla rahat',    2, 2::smallint),
    ('3', 'Duruma gore',         3, 3::smallint),
    ('4', 'Zorlanir',            4, 4::smallint),
    ('5', 'Cok zorlanir, aglar', 5, 5::smallint)
) AS v(code, label, ord, anx)
WHERE q.code IN ('Q4', 'Q4b', 'Q4c');


-- Q5 - sosyal egilim (24-72 ay).
-- Priors are onboarding hints only; the service clamps the result to [2.5, 3.5].
INSERT INTO question_options (question_id, code, label, display_order, social_orientation, gardner_priors)
SELECT q.id, v.code, v.label, v.ord, v.so, v.priors::jsonb
FROM questions q, (VALUES
    ('A', 'Hemen kaynasir',                  1, 'COOPERATIVE', '[{"domain":"INTERPERSONAL","delta":0.5}]'),
    ('B', 'Once izler, sonra katilir',       2, 'OBSERVER',    NULL),
    ('C', 'Yan yana ama kendi basina oynar', 3, 'PARALLEL',    '[{"domain":"INTRAPERSONAL","delta":0.5}]'),
    ('D', 'Yalniz oynamayi tercih eder',     4, 'SOLITARY',    '[{"domain":"INTRAPERSONAL","delta":0.5},{"domain":"INTERPERSONAL","delta":-0.5}]'),
    ('E', 'Oyunu yonetmek ister',            5, 'LEADER',      '[{"domain":"INTERPERSONAL","delta":0.5}]')
) AS v(code, label, ord, so, priors)
WHERE q.code = 'Q5';


-- Q6 - odak suresi (48-72 ay)
INSERT INTO question_options (question_id, code, label, display_order, focus_band)
SELECT q.id, v.code, v.label, v.ord, v.fb
FROM questions q, (VALUES
    ('A', '~5 dakika',    1, 'SHORT'),
    ('B', '10-15 dakika', 2, 'MEDIUM'),
    ('C', '20+ dakika',   3, 'LONG')
) AS v(code, label, ord, fb)
WHERE q.code = 'Q6';


-- Q7 - gunluk vakit (household scope -> parent_profiles)
INSERT INTO question_options (question_id, code, label, display_order, daily_time_budget_minutes)
SELECT q.id, v.code, v.label, v.ord, v.mins
FROM questions q, (VALUES
    ('A', '5-10 dk',  1, 10::smallint),
    ('B', '15-20 dk', 2, 20::smallint),
    ('C', '30+ dk',   3, 30::smallint)
) AS v(code, label, ord, mins)
WHERE q.code = 'Q7';
