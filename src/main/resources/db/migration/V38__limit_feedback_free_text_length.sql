UPDATE questions
SET max_length = 500
WHERE code = 'FB_COMMENT'
  AND scope = 'FEEDBACK';

-- Preserve any historical longer comments while enforcing the limit for every
-- new or updated row. Request validation supplies the user-facing error.
ALTER TABLE feedback
    ADD CONSTRAINT chk_feedback_free_text_length
        CHECK (free_text IS NULL OR char_length(free_text) <= 500)
        NOT VALID;
