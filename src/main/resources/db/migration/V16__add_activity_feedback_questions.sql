-- Activity feedback questions share the existing configurable question model,
-- but must never be included in onboarding questionnaire sessions.
ALTER TABLE questions DROP CONSTRAINT chk_questions_scope;

ALTER TABLE questions
    ADD CONSTRAINT chk_questions_scope
        CHECK (scope IN ('CHILD', 'HOUSEHOLD', 'IDENTITY', 'FEEDBACK'));

COMMENT ON COLUMN questions.scope IS
    'CHILD     - answer is folded into child_profile_snapshots.
     HOUSEHOLD - answered once per parent, written to parent_profiles.
     IDENTITY  - collected while creating a child.
     FEEDBACK  - shown after a selected daily-plan activity.';

INSERT INTO questions
    (code, body, helper_text, question_type, scope, max_length,
     min_age_months, max_age_months, required, display_order, is_active)
VALUES
    ('FB_ENJOYMENT',
     'Çocuğun etkinlikten keyif aldı mı?',
     NULL,
     'SINGLE_CHOICE', 'FEEDBACK', NULL,
     0, 72, TRUE, 1, TRUE),
    ('FB_EXPERIENCE',
     'Nasıl geçti?',
     'İstediğin kadar seçebilirsin, zorunlu değil.',
     'MULTI_CHOICE', 'FEEDBACK', NULL,
     0, 72, FALSE, 2, TRUE),
    ('FB_COMMENT',
     'Deneyiminizi kendi cümlelerinizle paylaşın.',
     'İsterseniz yazarak paylaşabilir veya sesli anlatabilirsiniz. Zorunlu değil.',
     'FREE_TEXT', 'FEEDBACK', 2000,
     0, 72, FALSE, 3, TRUE);

INSERT INTO question_options (question_id, code, label, display_order)
SELECT question.id, option.code, option.label, option.display_order
FROM questions question,
     (VALUES
         ('LOVED',          'Çok sevdi',         1),
         ('LIKED',          'Biraz sevdi',       2),
         ('NOT_INTERESTED', 'Pek ilgilenmedi',   3)
     ) AS option(code, label, display_order)
WHERE question.code = 'FB_ENJOYMENT';

INSERT INTO question_options (question_id, code, label, display_order)
SELECT question.id, option.code, option.label, option.display_order
FROM questions question,
     (VALUES
         ('EASY',             'Kolaydı',                         1),
         ('JUST_RIGHT',       'Tam kıvamındaydı',                2),
         ('A_LITTLE_HARD',    'Biraz zordu',                     3),
         ('WANTED_AGAIN',     'Tekrar yapmak istedi',            4),
         ('FOCUS_DIFFICULTY', 'Dikkatini sürdürmekte zorlandı',  5),
         ('HAD_FUN_TOGETHER', 'Birlikte çok eğlendik',           6)
     ) AS option(code, label, display_order)
WHERE question.code = 'FB_EXPERIENCE';
