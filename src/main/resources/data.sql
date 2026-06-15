-- Avatar columns (idempotent migration for existing databases)
ALTER TABLE players ADD COLUMN IF NOT EXISTS avatar_animal varchar(20);
ALTER TABLE players ADD COLUMN IF NOT EXISTS avatar_color varchar(30);
UPDATE players SET avatar_animal = 'cat' WHERE avatar_animal IS NULL;
UPDATE players SET avatar_color = '#f97316' WHERE avatar_color IS NULL;
ALTER TABLE players ALTER COLUMN avatar_animal SET NOT NULL;
ALTER TABLE players ALTER COLUMN avatar_color SET NOT NULL;

ALTER TABLE rounds ADD COLUMN IF NOT EXISTS tiebreak_revote boolean NOT NULL DEFAULT false;
ALTER TABLE rounds ADD COLUMN IF NOT EXISTS answer_phase_started_at timestamptz;

ALTER TABLE player_answers ADD COLUMN IF NOT EXISTS voted_answer_id bigint REFERENCES answers(id);

ALTER TABLE rounds DROP CONSTRAINT IF EXISTS rounds_status_check;
ALTER TABLE rounds ADD CONSTRAINT rounds_status_check CHECK (
  status IN (
    'WAITING_FOR_QUESTION',
    'WAITING_FOR_ANSWERS',
    'REVEALING',
    'COMPLETED'
  )
);

INSERT INTO question_categories (name)
VALUES ('Jedzenie'), ('Hobby'), ('Charakter'), ('Impreza'), ('Grupa')
ON CONFLICT (name) DO NOTHING;

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Jaka jest twoja ulubiona zupa?', 'GUESS_PLAYER_ANSWER', id, true FROM question_categories WHERE name = 'Jedzenie'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Jaka jest twoja ulubiona zupa?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Jaki film możesz oglądać bez końca?', 'GUESS_PLAYER_ANSWER', id, true FROM question_categories WHERE name = 'Hobby'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Jaki film możesz oglądać bez końca?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Co najbardziej poprawia ci humor?', 'GUESS_PLAYER_ANSWER', id, true FROM question_categories WHERE name = 'Charakter'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Co najbardziej poprawia ci humor?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Kto najprawdopodobniej zaśnie pierwszy na imprezie?', 'VOTE_PERSON', id, true FROM question_categories WHERE name = 'Impreza'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Kto najprawdopodobniej zaśnie pierwszy na imprezie?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Kto jest najbardziej chaotyczny w grupie?', 'VOTE_PERSON', id, true FROM question_categories WHERE name = 'Grupa'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Kto jest najbardziej chaotyczny w grupie?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Kto najczęściej spóźnia się na spotkania?', 'VOTE_PERSON', id, true FROM question_categories WHERE name = 'Grupa'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Kto najczęściej spóźnia się na spotkania?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Co ta osoba powiedziałaby po wygraniu miliona?', 'BEST_ANSWER', id, true FROM question_categories WHERE name = 'Charakter'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Co ta osoba powiedziałaby po wygraniu miliona?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Jaka byłaby najdziwniejsza wymówka tej osoby?', 'BEST_ANSWER', id, true FROM question_categories WHERE name = 'Impreza'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Jaka byłaby najdziwniejsza wymówka tej osoby?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Jaką supermoc ta osoba wybrałaby jako pierwszą?', 'BEST_ANSWER', id, true FROM question_categories WHERE name = 'Hobby'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Jaką supermoc ta osoba wybrałaby jako pierwszą?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Jakie pytanie najlepiej pasuje do tej grupy?', 'PLAYER_CREATES_QUESTION', id, true FROM question_categories WHERE name = 'Grupa'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Jakie pytanie najlepiej pasuje do tej grupy?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Jaki byłby idealny wieczór tej osoby?', 'REUSE_QUESTION', id, true FROM question_categories WHERE name = 'Impreza'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Jaki byłby idealny wieczór tej osoby?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Co ta osoba zrobiłaby z niespodziewaną wolną sobotą?', 'REUSE_QUESTION', id, true FROM question_categories WHERE name = 'Hobby'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Co ta osoba zrobiłaby z niespodziewaną wolną sobotą?' AND predefined = true);

INSERT INTO questions (content, round_type, category_id, predefined)
SELECT 'Który napój najlepiej pasuje do tej osoby?', 'REUSE_QUESTION', id, true FROM question_categories WHERE name = 'Jedzenie'
AND NOT EXISTS (SELECT 1 FROM questions WHERE content = 'Który napój najlepiej pasuje do tej osoby?' AND predefined = true);

INSERT INTO question_options (question_id, content, display_order)
SELECT q.id, opt.content, opt.display_order
FROM questions q
JOIN (VALUES
  ('Jaki byłby idealny wieczór tej osoby?', 'Film i pizza na kanapie', 0),
  ('Jaki byłby idealny wieczór tej osoby?', 'Spacer i rozmowa do późna', 1),
  ('Jaki byłby idealny wieczór tej osoby?', 'Impreza z muzyką na maxa', 2),
  ('Jaki byłby idealny wieczór tej osoby?', 'Książka, koc i herbata', 3),
  ('Co ta osoba zrobiłaby z niespodziewaną wolną sobotą?', 'Wyruszy w nieplanowaną podróż', 0),
  ('Co ta osoba zrobiłaby z niespodziewaną wolną sobotą?', 'Zorganizuje domowe sprzątanie', 1),
  ('Co ta osoba zrobiłaby z niespodziewaną wolną sobotą?', 'Ogląda seriale bez przerwy', 2),
  ('Co ta osoba zrobiłaby z niespodziewaną wolną sobotą?', 'Spotka się ze znajomymi', 3),
  ('Który napój najlepiej pasuje do tej osoby?', 'Kawa o poranku', 0),
  ('Który napój najlepiej pasuje do tej osoby?', 'Herbata z cytryną', 1),
  ('Który napój najlepiej pasuje do tej osoby?', 'Woda z lodem', 2),
  ('Który napój najlepiej pasuje do tej osoby?', 'Sok owocowy', 3)
) AS opt(question_content, content, display_order) ON q.content = opt.question_content
WHERE q.round_type = 'REUSE_QUESTION' AND q.predefined = true
AND NOT EXISTS (
  SELECT 1 FROM question_options existing WHERE existing.question_id = q.id
);
