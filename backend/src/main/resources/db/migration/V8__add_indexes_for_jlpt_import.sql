-- Accélère les mises à jour basées sur word_jmdict_seq
CREATE INDEX IF NOT EXISTS idx_word_jmdict_seq ON word(word_jmdict_seq) WHERE word_jlpt_id IS NULL;

-- Accélère les mises à jour basées sur word_japanese
CREATE INDEX IF NOT EXISTS idx_word_japanese ON word(word_japanese) WHERE word_jlpt_id IS NULL;