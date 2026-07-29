-- Keep the selected option FK for scoring while also storing its code in the
-- appropriately typed value column (numeric_value for numeric codes, otherwise
-- text_value). Raw non-option answers still require exactly one value.
ALTER TABLE child_answers DROP CONSTRAINT chk_child_answers_answer_form;

ALTER TABLE child_answers
    ADD CONSTRAINT chk_child_answers_answer_form CHECK (
        (
            question_option_id IS NOT NULL
            AND date_value IS NULL
            AND (numeric_value IS NOT NULL)::int + (text_value IS NOT NULL)::int <= 1
        )
        OR
        (
            question_option_id IS NULL
            AND (numeric_value IS NOT NULL)::int
              + (text_value IS NOT NULL)::int
              + (date_value IS NOT NULL)::int = 1
        )
    );
