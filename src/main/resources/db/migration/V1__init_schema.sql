-- =============================================================================
-- Kinloop — V1__init_schema.sql
-- Flyway baseline. Single clean schema (no data exists yet; no migration chain).
--
-- CONVENTIONS
--   * BIGSERIAL primary keys everywhere.
--   * TIMESTAMPTZ for all timestamps; created_at / updated_at on mutable tables.
--   * Soft delete via deleted_at on user-visible content; hard-delete CASCADE
--     is only reached by genuine administrative deletes.
--   * Enum-like values are VARCHAR + CHECK (readable in psql, no ALTER TYPE pain).
--   * Lists get their own table. Optional / scope-specific field groups get their
--     own 1:1 table. Anything mandatory for every row stays on the core table.
--
-- TABLES MARKED [RECONSTRUCTED] were rebuilt from discussion, not from your
-- original DDL. Diff them against your real V1 before running.
-- =============================================================================


-- =============================================================================
-- 0. SHARED HELPERS
-- =============================================================================

-- Keeps updated_at honest without relying on the application layer.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =============================================================================
-- 1. IDENTITY & AUTH                                          [RECONSTRUCTED]
-- =============================================================================

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL
        CHECK (role IN ('PARENT', 'WORKSHOP', 'ADMIN')),
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,

    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_role ON users (role) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Single-use tokens for the two-step registration flow.
CREATE TABLE email_verification_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL
        CONSTRAINT fk_evt_user REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_evt_token UNIQUE (token)
);

CREATE INDEX idx_evt_user ON email_verification_tokens (user_id);


-- MVP0: metadata logging only. No per-device token enforcement yet.
CREATE TABLE user_devices (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL
        CONSTRAINT fk_user_devices_user REFERENCES users (id) ON DELETE CASCADE,
    device_label VARCHAR(255),
    user_agent   TEXT,
    ip_address   INET,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_devices_user ON user_devices (user_id);


-- =============================================================================
-- 2. PROFILES                                                 [RECONSTRUCTED]
--    NOTE: the V1 bug Gulcin flagged — children.parent_id pointed at parents(id)
--    while the table was named parent_profiles. Resolved here: the table is
--    parent_profiles and every FK points at it.
-- =============================================================================

CREATE TABLE parent_profiles (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL
        CONSTRAINT fk_parent_profiles_user REFERENCES users (id) ON DELETE CASCADE,
    full_name    VARCHAR(255),
    phone        VARCHAR(30),
    city         VARCHAR(100),
    district     VARCHAR(100),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,

    CONSTRAINT uq_parent_profiles_user UNIQUE (user_id)
);

CREATE TRIGGER trg_parent_profiles_updated_at
    BEFORE UPDATE ON parent_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE workshop_profiles (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL
        CONSTRAINT fk_workshop_profiles_user REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    phone            VARCHAR(30),
    website          VARCHAR(255),
    city             VARCHAR(100),
    district         VARCHAR(100),
    address          TEXT,
    is_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,

    CONSTRAINT uq_workshop_profiles_user UNIQUE (user_id)
);

CREATE INDEX idx_workshop_profiles_city ON workshop_profiles (city, district);

CREATE TRIGGER trg_workshop_profiles_updated_at
    BEFORE UPDATE ON workshop_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- 3. CHILDREN                                                 [RECONSTRUCTED]
--    Core identity + the structural onboarding output the algorithm reads.
--    Raw answers stay in child_answers as an archive (Gulcin decision K4).
-- =============================================================================

CREATE TABLE children (
    id                        BIGSERIAL PRIMARY KEY,
    parent_id                 BIGINT      NOT NULL
        CONSTRAINT fk_children_parent REFERENCES parent_profiles (id) ON DELETE CASCADE,
    full_name                 VARCHAR(255) NOT NULL,
    birth_date                DATE        NOT NULL,
    gender                    VARCHAR(20)
        CHECK (gender IN ('FEMALE', 'MALE', 'OTHER', 'UNDISCLOSED')),
    notes                     TEXT,

    -- structural onboarding output (algorithm reads these, not child_answers)
    dunn_quadrant             VARCHAR(10)
        CHECK (dunn_quadrant IN ('C1', 'C2', 'C3', 'C4', 'MIXED')),
    noise_sensitivity         SMALLINT CHECK (noise_sensitivity     BETWEEN 1 AND 5),
    visual_sensitivity        SMALLINT CHECK (visual_sensitivity    BETWEEN 1 AND 5),
    mobility_preference       SMALLINT CHECK (mobility_preference   BETWEEN 1 AND 5),
    separation_anxiety        SMALLINT CHECK (separation_anxiety    BETWEEN 1 AND 5),
    social_orientation        VARCHAR(20)
        CHECK (social_orientation IN ('LEADER','COOPERATIVE','OBSERVER','PARALLEL','SOLITARY')),
    focus_band                VARCHAR(10)
        CHECK (focus_band IN ('SHORT', 'MEDIUM', 'LONG')),
    daily_time_budget_minutes SMALLINT CHECK (daily_time_budget_minutes > 0),
    preference_mode           VARCHAR(15) NOT NULL DEFAULT 'BALANCED'
        CHECK (preference_mode IN ('BALANCED', 'KEYIF', 'GELISIM')),
    onboarding_completed_at   TIMESTAMPTZ,

    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                TIMESTAMPTZ,

    CONSTRAINT chk_children_birth_date_past CHECK (birth_date <= CURRENT_DATE)
);

CREATE INDEX idx_children_parent     ON children (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_children_birth_date ON children (birth_date);

CREATE TRIGGER trg_children_updated_at
    BEFORE UPDATE ON children
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE child_allergies (
    id          BIGSERIAL PRIMARY KEY,
    child_id    BIGINT       NOT NULL
        CONSTRAINT fk_child_allergies_child REFERENCES children (id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    severity    VARCHAR(20)
        CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE')),
    notes       TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_child_allergies UNIQUE (child_id, name)
);

CREATE INDEX idx_child_allergies_child ON child_allergies (child_id);


CREATE TABLE child_medications (
    id          BIGSERIAL PRIMARY KEY,
    child_id    BIGINT       NOT NULL
        CONSTRAINT fk_child_medications_child REFERENCES children (id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    dosage      VARCHAR(100),
    frequency   VARCHAR(100),
    notes       TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_child_medications UNIQUE (child_id, name)
);

CREATE INDEX idx_child_medications_child ON child_medications (child_id);


CREATE TABLE consents (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL
        CONSTRAINT fk_consents_user REFERENCES users (id) ON DELETE CASCADE,
    child_id     BIGINT
        CONSTRAINT fk_consents_child REFERENCES children (id) ON DELETE CASCADE,
    consent_type VARCHAR(40) NOT NULL
        CHECK (consent_type IN ('TERMS', 'PRIVACY', 'KVKK', 'MARKETING', 'DATA_PROCESSING')),
    granted      BOOLEAN     NOT NULL,
    version      VARCHAR(20),
    granted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at   TIMESTAMPTZ
);

CREATE INDEX idx_consents_user ON consents (user_id, consent_type);


-- =============================================================================
-- 4. ONBOARDING QUESTION MODEL                                [RECONSTRUCTED]
--    Gulcin owns this. Raw answers are an archive; the algorithm reads the
--    structural columns on children.
-- =============================================================================

CREATE TABLE questions (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(50)  NOT NULL,
    body           TEXT         NOT NULL,
    question_type  VARCHAR(30)  NOT NULL
        CHECK (question_type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'SCALE', 'FREE_TEXT')),
    min_age_months INTEGER      NOT NULL DEFAULT 0  CHECK (min_age_months >= 0),
    max_age_months INTEGER      NOT NULL DEFAULT 72 CHECK (max_age_months >= 0),
    display_order  INTEGER      NOT NULL DEFAULT 0,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_questions_code UNIQUE (code),
    CONSTRAINT chk_questions_age_range CHECK (max_age_months >= min_age_months)
);

CREATE TRIGGER trg_questions_updated_at
    BEFORE UPDATE ON questions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE question_options (
    id            BIGSERIAL PRIMARY KEY,
    question_id   BIGINT      NOT NULL
        CONSTRAINT fk_question_options_question REFERENCES questions (id) ON DELETE CASCADE,
    code          VARCHAR(50) NOT NULL,
    label         TEXT        NOT NULL,
    display_order INTEGER     NOT NULL DEFAULT 0,
    -- how this option nudges the structural profile; interpreted by the service layer
    scoring_hint  JSONB       NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT uq_question_options UNIQUE (question_id, code)
);

CREATE INDEX idx_question_options_question ON question_options (question_id);


CREATE TABLE child_answers (
    id                 BIGSERIAL PRIMARY KEY,
    child_id           BIGINT      NOT NULL
        CONSTRAINT fk_child_answers_child REFERENCES children (id) ON DELETE CASCADE,
    question_id        BIGINT      NOT NULL
        CONSTRAINT fk_child_answers_question REFERENCES questions (id),
    question_option_id BIGINT
        CONSTRAINT fk_child_answers_option REFERENCES question_options (id),
    numeric_value      SMALLINT,
    text_value         TEXT,
    answered_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_child_answers UNIQUE (child_id, question_id, question_option_id)
);

CREATE INDEX idx_child_answers_child ON child_answers (child_id);


-- =============================================================================
-- 5. MEDIA                                                    [RECONSTRUCTED]
-- =============================================================================

CREATE TABLE media_assets (
    id            BIGSERIAL PRIMARY KEY,
    uploaded_by   BIGINT
        CONSTRAINT fk_media_assets_user REFERENCES users (id) ON DELETE SET NULL,
    storage_key   VARCHAR(500) NOT NULL,
    url           TEXT,
    mime_type     VARCHAR(100),
    size_bytes    BIGINT CHECK (size_bytes >= 0),
    width_px      INTEGER,
    height_px     INTEGER,
    alt_text      VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,

    CONSTRAINT uq_media_assets_storage_key UNIQUE (storage_key)
);


-- =============================================================================
-- 6. ACTIVITIES — core
--    One table for both scopes. scope and workshop_id are INDEPENDENT axes:
--      scope       = where the activity happens (HOME / WORKSHOP)
--      workshop_id = who authored it; NULL means platform/editorial content
--    A workshop may therefore author HOME activities.
--    Pedagogy fields live here because every activity must have them.
-- =============================================================================

CREATE TABLE activities (
    id                     BIGSERIAL PRIMARY KEY,

    -- identity
    scope                  VARCHAR(20)  NOT NULL
        CONSTRAINT chk_activities_scope CHECK (scope IN ('HOME', 'WORKSHOP')),
    workshop_id            BIGINT
        CONSTRAINT fk_activities_workshop
            REFERENCES workshop_profiles (id) ON DELETE CASCADE,
    title                  VARCHAR(255) NOT NULL,
    description            TEXT,
    min_age_months         INTEGER      NOT NULL CHECK (min_age_months >= 0),
    max_age_months         INTEGER      NOT NULL CHECK (max_age_months >= 0),
    status                 VARCHAR(30)  NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_activities_status
            CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'CANCELLED')),

    -- pedagogy: mandatory for every activity, both scopes
    target_intelligence    VARCHAR(30)  NOT NULL
        CHECK (target_intelligence IN ('VERBAL_LINGUISTIC','LOGICAL_MATHEMATICAL','MUSICAL',
            'VISUAL_SPATIAL','BODILY_KINAESTHETIC','INTERPERSONAL','INTRAPERSONAL','NATURALISTIC')),
    secondary_intelligence VARCHAR(30)
        CHECK (secondary_intelligence IN ('VERBAL_LINGUISTIC','LOGICAL_MATHEMATICAL','MUSICAL',
            'VISUAL_SPATIAL','BODILY_KINAESTHETIC','INTERPERSONAL','INTRAPERSONAL','NATURALISTIC')),
    target_domain          VARCHAR(30)  NOT NULL
        CHECK (target_domain IN ('GROSS_MOTOR','FINE_MOTOR','LANGUAGE','SOCIAL_EMOTIONAL','COGNITIVE')),
    difficulty             SMALLINT     NOT NULL CHECK (difficulty BETWEEN 1 AND 5),
    duration_minutes       SMALLINT     NOT NULL CHECK (duration_minutes > 0),
    involvement_type       VARCHAR(20)  NOT NULL
        CHECK (involvement_type IN ('BIRLIKTE', 'GOZETIMLI', 'BAGIMSIZ')),

    -- sensory load: NOT NULL DEFAULT 3 (neutral) so the engine never branches on null
    noise_load             SMALLINT     NOT NULL DEFAULT 3 CHECK (noise_load         BETWEEN 1 AND 5),
    visual_load            SMALLINT     NOT NULL DEFAULT 3 CHECK (visual_load        BETWEEN 1 AND 5),
    physical_intensity     SMALLINT     NOT NULL DEFAULT 3 CHECK (physical_intensity BETWEEN 1 AND 5),
    is_scaffolded          BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at             TIMESTAMPTZ,

    CONSTRAINT chk_activities_age_range
        CHECK (max_age_months >= min_age_months),
    -- workshop activities must belong to a workshop; home activities need not
    CONSTRAINT chk_activities_workshop_scope
        CHECK (scope = 'HOME' OR workshop_id IS NOT NULL),
    CONSTRAINT chk_activities_distinct_intelligence
        CHECK (secondary_intelligence IS DISTINCT FROM target_intelligence)
);

CREATE INDEX idx_activities_scope     ON activities (scope);
CREATE INDEX idx_activities_workshop  ON activities (workshop_id) WHERE workshop_id IS NOT NULL;
CREATE INDEX idx_activities_status    ON activities (status);
CREATE INDEX idx_activities_age_range ON activities (min_age_months, max_age_months);
CREATE INDEX idx_activities_scoring   ON activities (scope, target_intelligence, difficulty);
CREATE INDEX idx_activities_sensory   ON activities (noise_load, physical_intensity);

CREATE TRIGGER trg_activities_updated_at
    BEFORE UPDATE ON activities
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- 7. ACTIVITIES — scope-specific and optional detail
-- =============================================================================

-- Commerce and physical location. Only meaningful for scope = 'WORKSHOP'.
CREATE TABLE activity_workshop_details (
    activity_id      BIGINT PRIMARY KEY
        CONSTRAINT fk_workshop_details_activity
            REFERENCES activities (id) ON DELETE CASCADE,
    price            NUMERIC(10,2) CHECK (price >= 0),
    currency         VARCHAR(3)    NOT NULL DEFAULT 'TRY',
    capacity         INTEGER       CHECK (capacity > 0),
    language         VARCHAR(50),
    city             VARCHAR(100),
    district         VARCHAR(100),
    location_address TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_workshop_details_city ON activity_workshop_details (city, district);

CREATE TRIGGER trg_workshop_details_updated_at
    BEFORE UPDATE ON activity_workshop_details
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Parent-facing guidance. Optional (a workshop listing may not need it).
CREATE TABLE activity_instructions (
    activity_id   BIGINT PRIMARY KEY
        CONSTRAINT fk_instructions_activity
            REFERENCES activities (id) ON DELETE CASCADE,
    intro         TEXT,
    safety_notes  TEXT,
    cleanup_notes TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_instructions_updated_at
    BEFORE UPDATE ON activity_instructions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE activity_instruction_steps (
    id          BIGSERIAL PRIMARY KEY,
    activity_id BIGINT   NOT NULL
        CONSTRAINT fk_steps_activity REFERENCES activities (id) ON DELETE CASCADE,
    step_order  SMALLINT NOT NULL CHECK (step_order > 0),
    body        TEXT     NOT NULL,

    CONSTRAINT uq_steps_activity_order UNIQUE (activity_id, step_order)
);

CREATE INDEX idx_steps_activity ON activity_instruction_steps (activity_id);


CREATE TABLE activity_materials (
    id            BIGSERIAL PRIMARY KEY,
    activity_id   BIGINT       NOT NULL
        CONSTRAINT fk_materials_activity REFERENCES activities (id) ON DELETE CASCADE,
    name          VARCHAR(150) NOT NULL,
    category      VARCHAR(50),  -- musical instrument or painting
    quantity      VARCHAR(50),                       -- free text: "2 sheets", "a handful"
    is_optional   BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    note          TEXT,

    CONSTRAINT uq_materials_activity_name UNIQUE (activity_id, name)
);

CREATE INDEX idx_materials_activity ON activity_materials (activity_id);


-- Your original table, unchanged.
CREATE TABLE activity_media (
    id            BIGSERIAL PRIMARY KEY,
    activity_id   BIGINT      NOT NULL
        CONSTRAINT fk_activity_media_activity REFERENCES activities (id) ON DELETE CASCADE,
    media_id      BIGINT      NOT NULL
        CONSTRAINT fk_activity_media_media REFERENCES media_assets (id) ON DELETE CASCADE,
    display_order INTEGER     NOT NULL DEFAULT 0,
    is_cover      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_activity_media UNIQUE (activity_id, media_id)
);

CREATE INDEX idx_activity_media_activity_id ON activity_media (activity_id);

-- At most one cover image per activity — enforced, not assumed.
CREATE UNIQUE INDEX uq_activity_media_single_cover
    ON activity_media (activity_id) WHERE is_cover;


-- Your original table, unchanged. Naturally workshop-only
CREATE TABLE activity_sessions (
    id             BIGSERIAL PRIMARY KEY,
    activity_id    BIGINT      NOT NULL
        CONSTRAINT fk_activity_sessions_activity
            REFERENCES activities (id) ON DELETE CASCADE,
    start_datetime TIMESTAMPTZ NOT NULL,
    end_datetime   TIMESTAMPTZ NOT NULL,
    status         VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED'
        CONSTRAINT chk_activity_sessions_status
            CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,

    CONSTRAINT chk_activity_sessions_time_range CHECK (end_datetime > start_datetime)
);

CREATE INDEX idx_activity_sessions_activity_id    ON activity_sessions (activity_id);
CREATE INDEX idx_activity_sessions_start_datetime ON activity_sessions (start_datetime);

CREATE TRIGGER trg_activity_sessions_updated_at
    BEFORE UPDATE ON activity_sessions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- 8. FLEXIBLE ATTRIBUTES (EAV long tail)                      [RECONSTRUCTED]
--    The 11 scoring fields are real columns above. Everything else — theme,
--    material detail, tags — lives here. Hybrid by design.
-- =============================================================================

CREATE TABLE attribute_definitions (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50) NOT NULL,
    label         VARCHAR(150) NOT NULL,
    value_type    VARCHAR(20) NOT NULL
        CHECK (value_type IN ('TEXT', 'NUMBER', 'BOOLEAN', 'ENUM')),
    allowed_values JSONB,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_attribute_definitions_code UNIQUE (code)
);


CREATE TABLE activity_attributes (
    id                      BIGSERIAL PRIMARY KEY,
    activity_id             BIGINT NOT NULL
        CONSTRAINT fk_activity_attributes_activity
            REFERENCES activities (id) ON DELETE CASCADE,
    attribute_definition_id BIGINT NOT NULL
        CONSTRAINT fk_activity_attributes_definition
            REFERENCES attribute_definitions (id) ON DELETE CASCADE,
    value                   TEXT,

    CONSTRAINT uq_activity_attributes UNIQUE (activity_id, attribute_definition_id)
);

CREATE INDEX idx_activity_attributes_activity  ON activity_attributes (activity_id);
CREATE INDEX idx_activity_attributes_definition ON activity_attributes (attribute_definition_id);


-- =============================================================================
-- 9. ALGORITHM STATE — the engine's memory
--    Service layer INSERTs 8 intelligence rows and 5 domain rows per new child.
-- =============================================================================

CREATE TABLE child_intelligence_scores (
    id                BIGSERIAL PRIMARY KEY,
    child_id          BIGINT       NOT NULL
        CONSTRAINT fk_cis_child REFERENCES children (id) ON DELETE CASCADE,
    intelligence_type VARCHAR(30)  NOT NULL
        CHECK (intelligence_type IN ('VERBAL_LINGUISTIC','LOGICAL_MATHEMATICAL','MUSICAL',
            'VISUAL_SPATIAL','BODILY_KINAESTHETIC','INTERPERSONAL','INTRAPERSONAL','NATURALISTIC')),
    score             NUMERIC(3,2) NOT NULL DEFAULT 3.00
        CHECK (score BETWEEN 0.00 AND 5.00),
    feedback_count    INTEGER      NOT NULL DEFAULT 0 CHECK (feedback_count >= 0),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_cis_child_type UNIQUE (child_id, intelligence_type)
);

CREATE INDEX idx_cis_child ON child_intelligence_scores (child_id);


CREATE TABLE child_domain_levels (
    id         BIGSERIAL PRIMARY KEY,
    child_id   BIGINT      NOT NULL
        CONSTRAINT fk_cdl_child REFERENCES children (id) ON DELETE CASCADE,
    domain     VARCHAR(30) NOT NULL
        CHECK (domain IN ('GROSS_MOTOR','FINE_MOTOR','LANGUAGE','SOCIAL_EMOTIONAL','COGNITIVE')),
    level      SMALLINT    NOT NULL DEFAULT 1 CHECK (level  BETWEEN 1 AND 5),
    streak     SMALLINT    NOT NULL DEFAULT 0 CHECK (streak BETWEEN 0 AND 2),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_cdl_child_domain UNIQUE (child_id, domain)
);

CREATE INDEX idx_cdl_child ON child_domain_levels (child_id);


-- =============================================================================
-- 10. DAILY PLANS — the portfolio log
--     Feedback can only attach to a plan item (manipulation rule M1).
-- =============================================================================

CREATE TABLE daily_plans (
    id         BIGSERIAL PRIMARY KEY,
    child_id   BIGINT      NOT NULL
        CONSTRAINT fk_daily_plans_child REFERENCES children (id) ON DELETE CASCADE,
    plan_date  DATE        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_daily_plans_child_date UNIQUE (child_id, plan_date)
);

CREATE INDEX idx_daily_plans_child ON daily_plans (child_id, plan_date DESC);


CREATE TABLE daily_plan_items (
    id            BIGSERIAL PRIMARY KEY,
    daily_plan_id BIGINT       NOT NULL
        CONSTRAINT fk_dpi_plan REFERENCES daily_plans (id) ON DELETE CASCADE,
    activity_id   BIGINT       NOT NULL
        CONSTRAINT fk_dpi_activity REFERENCES activities (id),
    slot_type     VARCHAR(15)  NOT NULL
        CHECK (slot_type IN ('STRENGTHEN', 'DEVELOP', 'EXPLORE')),
    score         NUMERIC(5,2),
    source        VARCHAR(15)  NOT NULL DEFAULT 'RULE'
        CHECK (source IN ('RULE', 'LLM_RERANK')),
    completed_at  TIMESTAMPTZ,

    CONSTRAINT uq_dpi_plan_slot     UNIQUE (daily_plan_id, slot_type),
    CONSTRAINT uq_dpi_plan_activity UNIQUE (daily_plan_id, activity_id)
);

CREATE INDEX idx_dpi_plan     ON daily_plan_items (daily_plan_id);
CREATE INDEX idx_dpi_activity ON daily_plan_items (activity_id);


-- =============================================================================
-- 11. FEEDBACK                                                [RECONSTRUCTED]
--     Two channels in one table:
--       workshop review  -> rating + comment
--       home plan loop   -> daily_plan_item_id + feedback_type
-- =============================================================================

CREATE TABLE feedback (
    id                 BIGSERIAL PRIMARY KEY,
    child_id           BIGINT      NOT NULL
        CONSTRAINT fk_feedback_child REFERENCES children (id) ON DELETE CASCADE,
    activity_id        BIGINT
        CONSTRAINT fk_feedback_activity REFERENCES activities (id) ON DELETE SET NULL,
    daily_plan_item_id BIGINT
        CONSTRAINT fk_feedback_plan_item REFERENCES daily_plan_items (id) ON DELETE CASCADE,

    -- workshop review channel
    rating             SMALLINT CHECK (rating BETWEEN 1 AND 5),
    comment            TEXT,

    -- home plan channel
    feedback_type      VARCHAR(15)
        CHECK (feedback_type IN ('LIKED', 'STRUGGLED', 'DISLIKED')),
    resolved_reason    VARCHAR(15)
        CHECK (resolved_reason IN ('SENSORY', 'INVOLVEMENT', 'INTEREST')),

    accepted           BOOLEAN     NOT NULL DEFAULT TRUE,   -- M9
    bulk_flag          BOOLEAN     NOT NULL DEFAULT FALSE,  -- M6
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- M3: at most one feedback per plan item
    CONSTRAINT uq_feedback_plan_item UNIQUE (daily_plan_item_id),
    -- a feedback row must belong to one channel or the other
    CONSTRAINT chk_feedback_target
        CHECK (daily_plan_item_id IS NOT NULL OR activity_id IS NOT NULL)
);

CREATE INDEX idx_feedback_child    ON feedback (child_id, created_at DESC);
CREATE INDEX idx_feedback_activity ON feedback (activity_id);


-- M4: reversible delta log. Every score change is auditable and undoable.
CREATE TABLE feedback_effects (
    id                BIGSERIAL PRIMARY KEY,
    feedback_id       BIGINT       NOT NULL
        CONSTRAINT fk_fe_feedback REFERENCES feedback (id) ON DELETE CASCADE,
    intelligence_type VARCHAR(30)  NOT NULL
        CHECK (intelligence_type IN ('VERBAL_LINGUISTIC','LOGICAL_MATHEMATICAL','MUSICAL',
            'VISUAL_SPATIAL','BODILY_KINAESTHETIC','INTERPERSONAL','INTRAPERSONAL','NATURALISTIC')),
    delta             NUMERIC(3,2) NOT NULL,
    reversed_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_fe_feedback ON feedback_effects (feedback_id);
CREATE INDEX idx_fe_active   ON feedback_effects (feedback_id) WHERE reversed_at IS NULL;


-- =============================================================================
-- 12. RECOMMENDATIONS                                         [RECONSTRUCTED]
-- =============================================================================

CREATE TABLE recommendations (
    id              BIGSERIAL PRIMARY KEY,
    child_id        BIGINT       NOT NULL
        CONSTRAINT fk_recommendations_child REFERENCES children (id) ON DELETE CASCADE,
    activity_id     BIGINT       NOT NULL
        CONSTRAINT fk_recommendations_activity REFERENCES activities (id) ON DELETE CASCADE,
    score           NUMERIC(5,2) NOT NULL,
    rank            SMALLINT     CHECK (rank > 0),
    source          VARCHAR(15)  NOT NULL DEFAULT 'RULE'
        CHECK (source IN ('RULE', 'LLM_RERANK')),
    score_breakdown JSONB,          -- explainability: which rule contributed what
    explanation     TEXT,           -- LLM-generated, parent-facing
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendations_child ON recommendations (child_id, created_at DESC);


-- =============================================================================
-- 13. READ CONTRACT FOR THE SCORING ENGINE
--     The engine queries this view, never the tables directly. Drafts and
--     deleted rows are structurally invisible — not a discipline problem.
--     Both scopes are exposed; the engine weights scope itself.
-- =============================================================================

CREATE VIEW v_scorable_activities AS
SELECT
    a.id,
    a.scope,
    a.workshop_id,
    a.title,
    a.min_age_months,
    a.max_age_months,
    a.target_intelligence,
    a.secondary_intelligence,
    a.target_domain,
    a.difficulty,
    a.duration_minutes,
    a.involvement_type,
    a.noise_load,
    a.visual_load,
    a.physical_intensity,
    a.is_scaffolded
FROM activities a
WHERE a.status = 'PUBLISHED'
  AND a.deleted_at IS NULL;
