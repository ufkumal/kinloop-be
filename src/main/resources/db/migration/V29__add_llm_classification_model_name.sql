-- Records which model produced a feedback_llm_classifications row, so a
-- future model or prompt change can be A/B compared against prior results.
ALTER TABLE feedback_llm_classifications
    ADD COLUMN model_name VARCHAR(50);
