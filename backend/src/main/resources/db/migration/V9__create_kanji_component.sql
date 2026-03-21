-- V9 : Table de liaison kanji ↔ composants (radicaux visuels)
-- Permet la recherche multi-radical : kanji contenant composant A ET composant B
-- Source : KRADFILE / KRADFILE2 (EDRDG, CC BY-SA)

CREATE TABLE IF NOT EXISTS kanji_component (
    kc_id       SERIAL      PRIMARY KEY,
    kanji_id    INTEGER     NOT NULL REFERENCES kanji(kanji_id) ON DELETE CASCADE,
    radical_id  INTEGER     NOT NULL REFERENCES radical(rad_id)  ON DELETE CASCADE,
    kc_position SMALLINT    NOT NULL DEFAULT 0,
    UNIQUE (kanji_id, radical_id)
);

CREATE INDEX IF NOT EXISTS idx_kc_kanji_id   ON kanji_component(kanji_id);
CREATE INDEX IF NOT EXISTS idx_kc_radical_id ON kanji_component(radical_id);