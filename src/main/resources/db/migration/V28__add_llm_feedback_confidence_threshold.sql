-- llm_signal_confidence_threshold (0.60) predates the free-text feedback
-- classification spec and is superseded by llm_feedback_confidence_threshold.
-- Keep the old row because an external consumer may still reference it.
-- llm_signal_max_absolute_delta (0.30) already matches the Fren 1 delta cap
-- exactly and remains the shared cap for target_correction/secondary_hint.
INSERT INTO scoring_parameters (parameter_key, value, description) VALUES
    ('llm_feedback_confidence_threshold', 0.70,
     'Minimum confidence to apply free-text feedback classification signals (Kidloop_FewShot_Prompt.md Fren 3)');
