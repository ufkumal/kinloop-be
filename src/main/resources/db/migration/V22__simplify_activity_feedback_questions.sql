-- Align the configurable activity-feedback questions with the v6 feedback types.
-- Question 14 (FB_EXPERIENCE) is no longer presented; question 13 now maps
-- directly to the persisted LIKED / STRUGGLED / DISLIKED feedback values.

BEGIN;

DO $$
DECLARE
    affected_rows INTEGER;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM questions
        WHERE id = 13
          AND code = 'FB_ENJOYMENT'
    ) THEN
        RAISE EXCEPTION 'Expected question 13 to be FB_ENJOYMENT';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM questions
        WHERE id = 14
          AND code = 'FB_EXPERIENCE'
    ) THEN
        RAISE EXCEPTION 'Expected question 14 to be FB_EXPERIENCE';
    END IF;

    -- Keep this explicit even though deleting the question would cascade to
    -- its options, so the intended data removal remains visible and auditable.
    DELETE FROM question_options
    WHERE question_id = 14;

    DELETE FROM questions
    WHERE id = 14;

    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    IF affected_rows <> 1 THEN
        RAISE EXCEPTION 'Expected to delete question 14, deleted % rows', affected_rows;
    END IF;

    -- LIKED must move first because option codes are unique per question.
    UPDATE question_options
    SET code = 'STRUGGLED',
        label = 'Denedik, zorlandı'
    WHERE question_id = 13
      AND code = 'LIKED';

    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    IF affected_rows <> 1 THEN
        RAISE EXCEPTION 'Expected to update one LIKED option, updated % rows', affected_rows;
    END IF;

    UPDATE question_options
    SET code = 'LIKED',
        label = 'Yaptık, sevdi'
    WHERE question_id = 13
      AND code = 'LOVED';

    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    IF affected_rows <> 1 THEN
        RAISE EXCEPTION 'Expected to update one LOVED option, updated % rows', affected_rows;
    END IF;

    UPDATE question_options
    SET code = 'DISLIKED',
        label = 'Olmadı, sevmedi'
    WHERE question_id = 13
      AND code = 'NOT_INTERESTED';

    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    IF affected_rows <> 1 THEN
        RAISE EXCEPTION 'Expected to update one NOT_INTERESTED option, updated % rows', affected_rows;
    END IF;
END $$;

COMMIT;
