INSERT INTO scoring_parameters (parameter_key, value, description) VALUES
    ('llm_max_affected_domains', 3,
     'Maximum Gardner domains affected by one feedback: activity target, activity secondary, and LLM secondary hint')
ON CONFLICT (parameter_key) DO UPDATE
SET value = EXCLUDED.value,
    description = EXCLUDED.description;
