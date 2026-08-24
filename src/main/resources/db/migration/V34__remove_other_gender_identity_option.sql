-- OTHER is no longer offered for the optional child gender identity question.
DELETE FROM question_options qo
USING questions q
WHERE qo.question_id = q.id
  AND q.code = 'Q8'
  AND qo.code = 'OTHER';

-- Keep the remaining choices consecutively ordered for API consumers.
UPDATE question_options qo
SET display_order = 3
FROM questions q
WHERE qo.question_id = q.id
  AND q.code = 'Q8'
  AND qo.code = 'UNDISCLOSED';
