INSERT INTO scoring_parameters (parameter_key, value, description) VALUES
    ('llm_secondhand_confidence_cap', 0.60,
     'Fren 6: confidence ceiling when the parent is relaying a secondhand report (documentation cap for the system prompt; already subsumed by llm_feedback_confidence_threshold=0.70)'),
    ('llm_sensory_tolerance_step',    1.00,
     'Units the relevant sensory axis (or axes, for CROWDING) tightens by when sensory_hint is applied');
