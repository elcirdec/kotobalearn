CREATE TABLE word_example (
    we_id         SERIAL PRIMARY KEY,
    word_id       INTEGER NOT NULL REFERENCES word(word_id) ON DELETE CASCADE,
    we_japanese   TEXT    NOT NULL,
    we_english    TEXT    NOT NULL,
    we_tatoeba_id VARCHAR(20),
    we_form       VARCHAR(100)
);

CREATE INDEX idx_word_example_word ON word_example(word_id);