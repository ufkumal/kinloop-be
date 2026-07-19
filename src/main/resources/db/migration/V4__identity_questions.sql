-- =============================================================================
-- V4__identity_questions.sql
-- Kinloop - the child's name becomes a real question, and identity questions
--           carry enough metadata for the FE to render and submit them blind.
--
-- WHY answer_key
--   Without it the frontend must hardcode "Q0 goes in the fullName field".
--   That means adding or reordering an identity question needs a frontend
--   deploy - exactly what putting the questions in the database was meant to
--   avoid. With answer_key the FE loops over whatever the API returns and
--   builds the request body generically.
--
-- WHY helper_text
--   "Bu bilgiyi vermek zorunda değilsiniz" is wording, not logic. It belongs
--   next to the question it qualifies, so Gulcin can soften, translate or drop
--   it without a release.
--
-- ORDERING NOTE
--   chk_questions_identity_answer_key is added LAST, after every IDENTITY row
--   has an answer_key. Adding it earlier fails immediately: V3 already set
--   Q1.scope = 'IDENTITY' while its answer_key is still NULL.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Question metadata the renderer needs
-- -----------------------------------------------------------------------------
ALTER TABLE questions
    ADD COLUMN helper_text TEXT,
    ADD COLUMN answer_key  VARCHAR(50),
    ADD COLUMN max_length  INTEGER;

COMMENT ON COLUMN questions.helper_text IS
    'Secondary line rendered under the question. Wording only, never logic.';
COMMENT ON COLUMN questions.answer_key IS
    'For IDENTITY questions: the JSON field name in CreateChildRequest that this
     answer populates. Lets the FE build the request body without hardcoding
     question codes. NULL for CHILD and HOUSEHOLD questions, whose answers go to
     child_answers keyed by question code.';
COMMENT ON COLUMN questions.max_length IS
    'Client-side hint for FREE_TEXT questions. Server-side validation is
     independent and authoritative.';


-- -----------------------------------------------------------------------------
-- 2. Q0 - the child's name. Optional by design (required = FALSE).
-- -----------------------------------------------------------------------------
INSERT INTO questions
    (code, body, helper_text, question_type, scope, answer_key, max_length,
     min_age_months, max_age_months, required, display_order, is_active)
VALUES
    ('Q0',
     'Çocuğunuzun adı?',
     'Bu bilgiyi vermek zorunda değilsiniz.',
     'FREE_TEXT',
     'IDENTITY',
     'fullName',
     255,
     0, 72,
     FALSE,
     1,
     TRUE);


-- -----------------------------------------------------------------------------
-- 3. Q1 gets its answer_key and moves behind Q0 in the identity set.
--    (display_order is only compared within a scope, so this does not disturb
--     the CHILD questions that also use orders 1-7.)
-- -----------------------------------------------------------------------------
UPDATE questions
   SET answer_key    = 'birthDate',
       display_order = 2,
       body          = 'Çocuğunuzun doğum tarihi?'
 WHERE code = 'Q1';


-- -----------------------------------------------------------------------------
-- 4. Now that every IDENTITY row has an answer_key, enforce it.
--    An IDENTITY question without one cannot be submitted by the client.
-- -----------------------------------------------------------------------------
ALTER TABLE questions
    ADD CONSTRAINT chk_questions_identity_answer_key
        CHECK (scope <> 'IDENTITY' OR answer_key IS NOT NULL);


-- -----------------------------------------------------------------------------
-- 5. Verification
-- -----------------------------------------------------------------------------
-- SELECT code, display_order, answer_key, required, question_type, body
--   FROM questions WHERE scope = 'IDENTITY' AND is_active
--  ORDER BY display_order;
-- -> Q0 (fullName, FREE_TEXT, optional), Q1 (birthDate, DATE, required)
