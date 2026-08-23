INSERT INTO scoring_parameters (parameter_key, value, description) VALUES
    ('llm_difficulty_hint_harder_delta',  0.20,
     'Streak delta when LLM feedback classification indicates the child needs harder activities'),
    ('llm_difficulty_hint_easier_delta', -0.20,
     'Streak delta when LLM feedback classification indicates the child needs easier activities');
