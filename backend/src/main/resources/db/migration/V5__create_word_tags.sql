CREATE TABLE tag (
    tag_id    SERIAL       PRIMARY KEY,
    tag_code  VARCHAR(50)  NOT NULL,
    tag_type  VARCHAR(20)  NOT NULL,  -- pos, misc, field, ke_inf, dial
    tag_label TEXT         NOT NULL,
    UNIQUE (tag_code, tag_type)
);

CREATE TABLE word_tag (
    word_id INTEGER NOT NULL REFERENCES word(word_id)  ON DELETE CASCADE,
    tag_id  INTEGER NOT NULL REFERENCES tag(tag_id)    ON DELETE CASCADE,
    PRIMARY KEY (word_id, tag_id)
);

CREATE INDEX idx_word_tag_tag  ON word_tag(tag_id);
CREATE INDEX idx_word_tag_word ON word_tag(word_id);