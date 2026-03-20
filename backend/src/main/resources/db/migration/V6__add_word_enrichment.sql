-- V6 : Enrichissement des mots JMdict
-- word_jmdict_seq : numéro de séquence JMdict (ent_seq) pour le matching JLPT
-- word_frequency_rank : rang de fréquence (1=très courant, 2=courant, 3=assez courant, NULL=inconnu)

ALTER TABLE word ADD COLUMN IF NOT EXISTS word_jmdict_seq INTEGER;
ALTER TABLE word ADD COLUMN IF NOT EXISTS word_frequency_rank SMALLINT;

CREATE INDEX IF NOT EXISTS idx_word_jmdict_seq ON word(word_jmdict_seq);
CREATE INDEX IF NOT EXISTS idx_word_frequency_rank ON word(word_frequency_rank);