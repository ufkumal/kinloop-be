-- Q8 is optional identity data collected once for each child.
-- Option codes intentionally match the Gender enum so the frontend can send
-- the selected code directly as CreateChildRequest.gender.
INSERT INTO questions
    (code, body, helper_text, question_type, scope, answer_key,
     min_age_months, max_age_months, required, display_order, is_active)
VALUES
    ('Q8',
     'Çocuğunuzun cinsiyeti?',
     'Bu bilgiyi vermek zorunda değilsiniz.',
     'SINGLE_CHOICE',
     'IDENTITY',
     'gender',
     0, 72,
     FALSE,
     3,
     TRUE);

INSERT INTO question_options (question_id, code, label, display_order)
SELECT q.id, option_data.code, option_data.label, option_data.display_order
FROM questions q
CROSS JOIN (VALUES
    ('FEMALE',      'Kız',                 1),
    ('MALE',        'Erkek',               2),
    ('OTHER',       'Diğer',               3),
    ('UNDISCLOSED', 'Belirtmek istemiyorum', 4)
) AS option_data(code, label, display_order)
WHERE q.code = 'Q8';
