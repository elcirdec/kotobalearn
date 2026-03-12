-- ============================================================
-- KOTOBALEARN - SCHEMA INITIAL
-- Version : V1
-- Source principale MVP : KanjiAlive
-- Extensible : Kanjidic2, JMdict (phases suivantes)
-- ============================================================


-- ============================================================
-- 1. TABLES DE RÉFÉRENCE
-- ============================================================

CREATE TABLE language (
    lang_id   SERIAL PRIMARY KEY,
    lang_code VARCHAR(10) UNIQUE NOT NULL,
    lang_name VARCHAR(50)        NOT NULL
);

CREATE TABLE jlpt_level (
    jlpt_id          SERIAL PRIMARY KEY,
    jlpt_code        VARCHAR(5)  UNIQUE NOT NULL,
    jlpt_description VARCHAR(100)
);

CREATE TABLE category (
    cat_id   SERIAL PRIMARY KEY,
    cat_name VARCHAR(50) UNIQUE NOT NULL
);


-- ============================================================
-- 2. RADICAUX
-- ============================================================

CREATE TABLE radical (
    rad_id                SERIAL PRIMARY KEY,
    rad_character         VARCHAR(10)  NOT NULL,
    rad_strokes           INT          NOT NULL,
    rad_image_url         VARCHAR(255),
    rad_name_hiragana     VARCHAR(50),
    rad_name_romaji       VARCHAR(50),
    rad_meaning_english   TEXT,
    rad_position_hiragana VARCHAR(20),
    rad_position_romaji   VARCHAR(20),
    rad_position_icon_url VARCHAR(255)
);

CREATE INDEX idx_radical_character ON radical(rad_character);

CREATE TABLE radical_animation (
    ra_id        SERIAL PRIMARY KEY,
    rad_id       INT          NOT NULL REFERENCES radical(rad_id) ON DELETE CASCADE,
    ra_order     INT          NOT NULL,
    ra_image_url VARCHAR(255) NOT NULL
);

CREATE INDEX idx_radical_animation_rad ON radical_animation(rad_id);


-- ============================================================
-- 3. KANJI
-- ============================================================

CREATE TABLE kanji (
    kanji_id              SERIAL PRIMARY KEY,
    kanji_character       VARCHAR(10) UNIQUE NOT NULL,
    kanji_meaning_english TEXT,
    kanji_strokes         INT         NOT NULL,
    kanji_video_poster_url VARCHAR(255),
    kanji_video_mp4_url    VARCHAR(255),
    kanji_video_webm_url   VARCHAR(255),
    kanji_grade            INT,
    kanji_kodansha         VARCHAR(20),
    kanji_classic_nelson   VARCHAR(20),
    rad_id   INT REFERENCES radical(rad_id),
    jlpt_id  INT REFERENCES jlpt_level(jlpt_id)
);

CREATE INDEX idx_kanji_character ON kanji(kanji_character);
CREATE INDEX idx_kanji_jlpt      ON kanji(jlpt_id);
CREATE INDEX idx_kanji_grade     ON kanji(kanji_grade);


-- ============================================================
-- 4. LECTURES DES KANJI
-- ============================================================

CREATE TABLE reading (
    read_id     SERIAL PRIMARY KEY,
    kanji_id    INT         NOT NULL REFERENCES kanji(kanji_id) ON DELETE CASCADE,
    read_type   VARCHAR(10) NOT NULL CHECK (read_type IN ('ON', 'KUN')),
    read_kana   VARCHAR(50) NOT NULL,
    read_romaji VARCHAR(50) NOT NULL
);

CREATE INDEX idx_reading_kanji ON reading(kanji_id);


-- ============================================================
-- 5. EXEMPLES PAR KANJI (avec audio)
-- ============================================================

CREATE TABLE example (
    ex_id              SERIAL PRIMARY KEY,
    kanji_id           INT          NOT NULL REFERENCES kanji(kanji_id) ON DELETE CASCADE,
    ex_japanese        VARCHAR(255) NOT NULL,
    ex_meaning_english TEXT,
    ex_audio_opus_url  VARCHAR(255),
    ex_audio_aac_url   VARCHAR(255),
    ex_audio_ogg_url   VARCHAR(255),
    ex_audio_mp3_url   VARCHAR(255)
);

CREATE INDEX idx_example_kanji ON example(kanji_id);


-- ============================================================
-- 6. SYLLABAIRES HIRAGANA / KATAKANA
-- ============================================================

CREATE TABLE script (
    sc_id        SERIAL PRIMARY KEY,
    sc_type      VARCHAR(10) NOT NULL CHECK (sc_type IN ('HIRAGANA', 'KATAKANA')),
    sc_character VARCHAR(10) NOT NULL UNIQUE,
    sc_romaji    VARCHAR(20) NOT NULL
);

CREATE INDEX idx_script_type      ON script(sc_type);
CREATE INDEX idx_script_character ON script(sc_character);


-- ============================================================
-- 7. VOCABULAIRE (phase 2 — JMdict)
-- ============================================================

CREATE TABLE word (
    word_id                     SERIAL PRIMARY KEY,
    word_japanese               VARCHAR(255) NOT NULL,
    word_pronunciation_hiragana VARCHAR(255) NOT NULL,
    word_romaji                 VARCHAR(255) NOT NULL,
    word_translation_en         TEXT,          -- Colonne principale MVP (anglais)
    word_translation_fr         TEXT,          -- Remplie via table translation plus tard
    word_jlpt_id  INT REFERENCES jlpt_level(jlpt_id)
    -- ⚠️ Pas de word_category_id ici : remplacé par word_category (many-to-many)
);

CREATE INDEX idx_word_jlpt ON word(word_jlpt_id);


-- ============================================================
-- 8. LIAISON MOTS ↔ CATÉGORIES (many-to-many)
--    Un mot peut appartenir à plusieurs catégories
--    ex: 林檎 (pomme) → fruit ET aliment
-- ============================================================

CREATE TABLE word_category (
    wc_id   SERIAL PRIMARY KEY,
    word_id INT NOT NULL REFERENCES word(word_id)     ON DELETE CASCADE,
    cat_id  INT NOT NULL REFERENCES category(cat_id)  ON DELETE CASCADE,
    UNIQUE (word_id, cat_id)
);

CREATE INDEX idx_word_category_word ON word_category(word_id);
CREATE INDEX idx_word_category_cat  ON word_category(cat_id);


-- ============================================================
-- 9. LIAISON MOTS ↔ KANJI (many-to-many)
-- ============================================================

CREATE TABLE word_kanji (
    wk_id    SERIAL PRIMARY KEY,
    word_id  INT NOT NULL REFERENCES word(word_id)   ON DELETE CASCADE,
    kanji_id INT NOT NULL REFERENCES kanji(kanji_id) ON DELETE CASCADE,
    wk_order INT,
    UNIQUE (word_id, kanji_id)
);

CREATE INDEX idx_word_kanji_word  ON word_kanji(word_id);
CREATE INDEX idx_word_kanji_kanji ON word_kanji(kanji_id);


-- ============================================================
-- 10. TRADUCTIONS (hybride : colonnes _english conservées,
--     cette table pour le français et les langues futures)
--
--     trans_element_type : 'KANJI', 'EXAMPLE', 'RADICAL', 'WORD'
--     trans_element_id   : ID dans la table correspondante
--     lang_id            : référence vers language
--
--     Exemple d'utilisation :
--       SELECT trans_text FROM translation
--       WHERE trans_element_type = 'KANJI'
--         AND trans_element_id = 42
--         AND lang_id = (SELECT lang_id FROM language WHERE lang_code = 'fr');
-- ============================================================

CREATE TABLE translation (
    trans_id           SERIAL PRIMARY KEY,
    trans_element_type VARCHAR(20) NOT NULL
        CHECK (trans_element_type IN ('KANJI', 'EXAMPLE', 'RADICAL', 'WORD')),
    trans_element_id   INT  NOT NULL,
    lang_id            INT  NOT NULL REFERENCES language(lang_id),
    trans_text         TEXT NOT NULL,
    UNIQUE (trans_element_type, trans_element_id, lang_id)
);

CREATE INDEX idx_translation_element ON translation(trans_element_type, trans_element_id);
CREATE INDEX idx_translation_lang    ON translation(lang_id);


-- ============================================================
-- 11. UTILISATEURS
-- ============================================================

CREATE TABLE utilisateur (
    usr_id         SERIAL PRIMARY KEY,
    usr_pseudo     VARCHAR(50)  UNIQUE NOT NULL,
    usr_email      VARCHAR(150) UNIQUE NOT NULL,
    usr_mdp        VARCHAR(255) NOT NULL,
    usr_role       VARCHAR(20)  NOT NULL DEFAULT 'USER'
                   CHECK (usr_role IN ('USER', 'ADMIN')),
    usr_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 12. PROGRESSION PAR ÉLÉMENT
-- ============================================================

CREATE TABLE progress (
    prog_id           SERIAL PRIMARY KEY,
    usr_id            INT NOT NULL REFERENCES utilisateur(usr_id) ON DELETE CASCADE,
    prog_element_type VARCHAR(20) NOT NULL
        CHECK (prog_element_type IN ('KANJI', 'WORD', 'HIRAGANA', 'KATAKANA')),
    prog_element_id   INT NOT NULL,
    prog_status       VARCHAR(20) NOT NULL DEFAULT 'NOT_SEEN'
        CHECK (prog_status IN ('NOT_SEEN', 'SEEN', 'LEARNING', 'MASTERED')),
    prog_last_reviewed TIMESTAMP,
    prog_next_review   TIMESTAMP,
    prog_correct_count INT NOT NULL DEFAULT 0,
    prog_wrong_count   INT NOT NULL DEFAULT 0,
    CONSTRAINT unique_user_element UNIQUE (usr_id, prog_element_type, prog_element_id)
);

CREATE INDEX idx_progress_user        ON progress(usr_id);
CREATE INDEX idx_progress_next_review ON progress(prog_next_review)
    WHERE prog_next_review IS NOT NULL;


-- ============================================================
-- 13. SESSIONS DE QUIZ
-- ============================================================

CREATE TABLE quiz_session (
    qs_id         SERIAL PRIMARY KEY,
    usr_id        INT       NOT NULL REFERENCES utilisateur(usr_id) ON DELETE CASCADE,
    qs_start_time TIMESTAMP NOT NULL,
    qs_end_time   TIMESTAMP,
    qs_type       VARCHAR(100)
);

CREATE INDEX idx_quiz_session_user ON quiz_session(usr_id);


-- ============================================================
-- 14. QUESTIONS D'UN QUIZ
-- ============================================================

CREATE TABLE quiz_question (
    qq_id            SERIAL PRIMARY KEY,
    qs_id            INT     NOT NULL REFERENCES quiz_session(qs_id) ON DELETE CASCADE,
    qq_element_type  VARCHAR(20) NOT NULL
        CHECK (qq_element_type IN ('KANJI', 'WORD', 'HIRAGANA', 'KATAKANA')),
    qq_element_id    INT NOT NULL,
    qq_question_type VARCHAR(20) NOT NULL
        CHECK (qq_question_type IN ('QCM', 'TYPING', 'ASSOCIATION')),
    qq_user_answer   TEXT,
    qq_is_correct    BOOLEAN,
    qq_time_taken    INT
);

CREATE INDEX idx_quiz_question_session ON quiz_question(qs_id);


-- ============================================================
-- 15. DONNÉES DE RÉFÉRENCE INITIALES
-- ============================================================

INSERT INTO language (lang_code, lang_name) VALUES
    ('fr', 'Français'),
    ('en', 'English'),
    ('ja', '日本語');

INSERT INTO jlpt_level (jlpt_code, jlpt_description) VALUES
    ('N5', 'Débutant — ~100 kanji, ~800 mots'),
    ('N4', 'Élémentaire — ~300 kanji, ~1500 mots'),
    ('N3', 'Intermédiaire — ~650 kanji, ~3750 mots'),
    ('N2', 'Avancé — ~1000 kanji, ~6000 mots'),
    ('N1', 'Expert — ~2000 kanji, ~10000 mots');

INSERT INTO category (cat_name) VALUES
    ('noun'), ('verb'), ('adjective'), ('adverb'),
    ('counter'), ('expression'), ('particle'),
    ('food'), ('animal'), ('body'), ('color'),
    ('number'), ('time'), ('nature'), ('place');