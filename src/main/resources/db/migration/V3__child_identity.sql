-- =============================================================================
-- V3__child_identity.sql
-- Kinloop - Q1 becomes child creation, not a questionnaire answer
--
-- WHY
--   questionnaire_sessions.child_id is NOT NULL, so a session cannot exist
--   before the child does - and the child cannot exist without a birth date.
--   Q1 therefore precedes every session it would supposedly belong to.
--
--   Birth date is also immutable in a way no other answer is. Re-asking it each
--   age band invites a parent to "correct" it later, silently shifting every
--   historical age calculation and every snapshot's age_months_at_capture.
--
--   So Q1 keeps its row (Gulcin's matrix stays 1:1, the FE renders its label
--   from the same source) but gains scope IDENTITY: collected once at child
--   creation, never included in a session, never written to child_answers.
--
-- EFFECT ON QUESTION SETS (Matris 1.0 minus Q1)
--   0-12   -> Q2b, Q4b, Q7          (3)
--   12-24  -> Q2,  Q3, Q4, Q7       (4)
--   24-48  -> Q2,  Q3, Q4, Q5, Q7   (5)
--   48-72  -> Q2,  Q3, Q4c, Q5, Q6, Q7 (6)
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. A parent may decline to share the child's name.
--    Stays NULL rather than being auto-filled: null is the honest record of
--    "not given". The display fallback is computed at read time.
-- -----------------------------------------------------------------------------
ALTER TABLE children
    ALTER COLUMN full_name DROP NOT NULL;

COMMENT ON COLUMN children.full_name IS
    'Optional. NULL means the parent declined to share it. Never auto-populate
     this column - ChildService.displayName() derives a label for the UI.';


-- -----------------------------------------------------------------------------
-- 2. Third question scope: IDENTITY.
--    CHILD    -> folded into child_profile_snapshots
--    HOUSEHOLD-> written to parent_profiles (Q7)
--    IDENTITY -> written to children at creation (Q1)
-- -----------------------------------------------------------------------------
ALTER TABLE questions DROP CONSTRAINT chk_questions_scope;

ALTER TABLE questions
    ADD CONSTRAINT chk_questions_scope
        CHECK (scope IN ('CHILD', 'HOUSEHOLD', 'IDENTITY'));

UPDATE questions SET scope = 'IDENTITY' WHERE code = 'Q1';

COMMENT ON COLUMN questions.scope IS
    'CHILD    - answer is folded into child_profile_snapshots.
     HOUSEHOLD- answered once per parent, written to parent_profiles (Q7).
     IDENTITY - collected at child creation, written to children (Q1).
                Excluded from every questionnaire session.';


-- -----------------------------------------------------------------------------
-- 3. Verification (run manually, expect the counts in the header above)
-- -----------------------------------------------------------------------------
-- SELECT code, scope, min_age_months, max_age_months
--   FROM questions
--  WHERE is_active AND scope = 'CHILD'
--    AND 18 >= min_age_months AND 18 < max_age_months
--  ORDER BY display_order;
-- -> Q2, Q3, Q4   (Q7 is HOUSEHOLD, asked separately; Q1 is IDENTITY)
