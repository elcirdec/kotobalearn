-- V3 — Hiragana et Katakana
-- 46 hiragana de base + dakuten + handakuten + combinaisons (yōon)
-- 46 katakana de base + dakuten + handakuten + combinaisons (yōon)

-- ============================================================
-- HIRAGANA — base (46)
-- ============================================================
INSERT INTO script (sc_type, sc_character, sc_romaji) VALUES
('HIRAGANA', 'あ', 'a'),
('HIRAGANA', 'い', 'i'),
('HIRAGANA', 'う', 'u'),
('HIRAGANA', 'え', 'e'),
('HIRAGANA', 'お', 'o'),

('HIRAGANA', 'か', 'ka'),
('HIRAGANA', 'き', 'ki'),
('HIRAGANA', 'く', 'ku'),
('HIRAGANA', 'け', 'ke'),
('HIRAGANA', 'こ', 'ko'),

('HIRAGANA', 'さ', 'sa'),
('HIRAGANA', 'し', 'shi'),
('HIRAGANA', 'す', 'su'),
('HIRAGANA', 'せ', 'se'),
('HIRAGANA', 'そ', 'so'),

('HIRAGANA', 'た', 'ta'),
('HIRAGANA', 'ち', 'chi'),
('HIRAGANA', 'つ', 'tsu'),
('HIRAGANA', 'て', 'te'),
('HIRAGANA', 'と', 'to'),

('HIRAGANA', 'な', 'na'),
('HIRAGANA', 'に', 'ni'),
('HIRAGANA', 'ぬ', 'nu'),
('HIRAGANA', 'ね', 'ne'),
('HIRAGANA', 'の', 'no'),

('HIRAGANA', 'は', 'ha'),
('HIRAGANA', 'ひ', 'hi'),
('HIRAGANA', 'ふ', 'fu'),
('HIRAGANA', 'へ', 'he'),
('HIRAGANA', 'ほ', 'ho'),

('HIRAGANA', 'ま', 'ma'),
('HIRAGANA', 'み', 'mi'),
('HIRAGANA', 'む', 'mu'),
('HIRAGANA', 'め', 'me'),
('HIRAGANA', 'も', 'mo'),

('HIRAGANA', 'や', 'ya'),
('HIRAGANA', 'ゆ', 'yu'),
('HIRAGANA', 'よ', 'yo'),

('HIRAGANA', 'ら', 'ra'),
('HIRAGANA', 'り', 'ri'),
('HIRAGANA', 'る', 'ru'),
('HIRAGANA', 'れ', 're'),
('HIRAGANA', 'ろ', 'ro'),

('HIRAGANA', 'わ', 'wa'),
('HIRAGANA', 'を', 'wo'),
('HIRAGANA', 'ん', 'n');

-- ============================================================
-- HIRAGANA — dakuten (voiced) が〜ぽ
-- ============================================================
INSERT INTO script (sc_type, sc_character, sc_romaji) VALUES
('HIRAGANA', 'が', 'ga'),
('HIRAGANA', 'ぎ', 'gi'),
('HIRAGANA', 'ぐ', 'gu'),
('HIRAGANA', 'げ', 'ge'),
('HIRAGANA', 'ご', 'go'),

('HIRAGANA', 'ざ', 'za'),
('HIRAGANA', 'じ', 'ji'),
('HIRAGANA', 'ず', 'zu'),
('HIRAGANA', 'ぜ', 'ze'),
('HIRAGANA', 'ぞ', 'zo'),

('HIRAGANA', 'だ', 'da'),
('HIRAGANA', 'ぢ', 'di'),
('HIRAGANA', 'づ', 'du'),
('HIRAGANA', 'で', 'de'),
('HIRAGANA', 'ど', 'do'),

('HIRAGANA', 'ば', 'ba'),
('HIRAGANA', 'び', 'bi'),
('HIRAGANA', 'ぶ', 'bu'),
('HIRAGANA', 'べ', 'be'),
('HIRAGANA', 'ぼ', 'bo'),

-- handakuten (semi-voiced)
('HIRAGANA', 'ぱ', 'pa'),
('HIRAGANA', 'ぴ', 'pi'),
('HIRAGANA', 'ぷ', 'pu'),
('HIRAGANA', 'ぺ', 'pe'),
('HIRAGANA', 'ぽ', 'po');

-- ============================================================
-- HIRAGANA — combinaisons yōon
-- ============================================================
INSERT INTO script (sc_type, sc_character, sc_romaji) VALUES
('HIRAGANA', 'きゃ', 'kya'),
('HIRAGANA', 'きゅ', 'kyu'),
('HIRAGANA', 'きょ', 'kyo'),

('HIRAGANA', 'しゃ', 'sha'),
('HIRAGANA', 'しゅ', 'shu'),
('HIRAGANA', 'しょ', 'sho'),

('HIRAGANA', 'ちゃ', 'cha'),
('HIRAGANA', 'ちゅ', 'chu'),
('HIRAGANA', 'ちょ', 'cho'),

('HIRAGANA', 'にゃ', 'nya'),
('HIRAGANA', 'にゅ', 'nyu'),
('HIRAGANA', 'にょ', 'nyo'),

('HIRAGANA', 'ひゃ', 'hya'),
('HIRAGANA', 'ひゅ', 'hyu'),
('HIRAGANA', 'ひょ', 'hyo'),

('HIRAGANA', 'みゃ', 'mya'),
('HIRAGANA', 'みゅ', 'myu'),
('HIRAGANA', 'みょ', 'myo'),

('HIRAGANA', 'りゃ', 'rya'),
('HIRAGANA', 'りゅ', 'ryu'),
('HIRAGANA', 'りょ', 'ryo'),

('HIRAGANA', 'ぎゃ', 'gya'),
('HIRAGANA', 'ぎゅ', 'gyu'),
('HIRAGANA', 'ぎょ', 'gyo'),

('HIRAGANA', 'じゃ', 'ja'),
('HIRAGANA', 'じゅ', 'ju'),
('HIRAGANA', 'じょ', 'jo'),

('HIRAGANA', 'びゃ', 'bya'),
('HIRAGANA', 'びゅ', 'byu'),
('HIRAGANA', 'びょ', 'byo'),

('HIRAGANA', 'ぴゃ', 'pya'),
('HIRAGANA', 'ぴゅ', 'pyu'),
('HIRAGANA', 'ぴょ', 'pyo');

-- ============================================================
-- KATAKANA — base (46)
-- ============================================================
INSERT INTO script (sc_type, sc_character, sc_romaji) VALUES
('KATAKANA', 'ア', 'a'),
('KATAKANA', 'イ', 'i'),
('KATAKANA', 'ウ', 'u'),
('KATAKANA', 'エ', 'e'),
('KATAKANA', 'オ', 'o'),

('KATAKANA', 'カ', 'ka'),
('KATAKANA', 'キ', 'ki'),
('KATAKANA', 'ク', 'ku'),
('KATAKANA', 'ケ', 'ke'),
('KATAKANA', 'コ', 'ko'),

('KATAKANA', 'サ', 'sa'),
('KATAKANA', 'シ', 'shi'),
('KATAKANA', 'ス', 'su'),
('KATAKANA', 'セ', 'se'),
('KATAKANA', 'ソ', 'so'),

('KATAKANA', 'タ', 'ta'),
('KATAKANA', 'チ', 'chi'),
('KATAKANA', 'ツ', 'tsu'),
('KATAKANA', 'テ', 'te'),
('KATAKANA', 'ト', 'to'),

('KATAKANA', 'ナ', 'na'),
('KATAKANA', 'ニ', 'ni'),
('KATAKANA', 'ヌ', 'nu'),
('KATAKANA', 'ネ', 'ne'),
('KATAKANA', 'ノ', 'no'),

('KATAKANA', 'ハ', 'ha'),
('KATAKANA', 'ヒ', 'hi'),
('KATAKANA', 'フ', 'fu'),
('KATAKANA', 'ヘ', 'he'),
('KATAKANA', 'ホ', 'ho'),

('KATAKANA', 'マ', 'ma'),
('KATAKANA', 'ミ', 'mi'),
('KATAKANA', 'ム', 'mu'),
('KATAKANA', 'メ', 'me'),
('KATAKANA', 'モ', 'mo'),

('KATAKANA', 'ヤ', 'ya'),
('KATAKANA', 'ユ', 'yu'),
('KATAKANA', 'ヨ', 'yo'),

('KATAKANA', 'ラ', 'ra'),
('KATAKANA', 'リ', 'ri'),
('KATAKANA', 'ル', 'ru'),
('KATAKANA', 'レ', 're'),
('KATAKANA', 'ロ', 'ro'),

('KATAKANA', 'ワ', 'wa'),
('KATAKANA', 'ヲ', 'wo'),
('KATAKANA', 'ン', 'n');

-- ============================================================
-- KATAKANA — dakuten (voiced)
-- ============================================================
INSERT INTO script (sc_type, sc_character, sc_romaji) VALUES
('KATAKANA', 'ガ', 'ga'),
('KATAKANA', 'ギ', 'gi'),
('KATAKANA', 'グ', 'gu'),
('KATAKANA', 'ゲ', 'ge'),
('KATAKANA', 'ゴ', 'go'),

('KATAKANA', 'ザ', 'za'),
('KATAKANA', 'ジ', 'ji'),
('KATAKANA', 'ズ', 'zu'),
('KATAKANA', 'ゼ', 'ze'),
('KATAKANA', 'ゾ', 'zo'),

('KATAKANA', 'ダ', 'da'),
('KATAKANA', 'ヂ', 'di'),
('KATAKANA', 'ヅ', 'du'),
('KATAKANA', 'デ', 'de'),
('KATAKANA', 'ド', 'do'),

('KATAKANA', 'バ', 'ba'),
('KATAKANA', 'ビ', 'bi'),
('KATAKANA', 'ブ', 'bu'),
('KATAKANA', 'ベ', 'be'),
('KATAKANA', 'ボ', 'bo'),

-- handakuten
('KATAKANA', 'パ', 'pa'),
('KATAKANA', 'ピ', 'pi'),
('KATAKANA', 'プ', 'pu'),
('KATAKANA', 'ペ', 'pe'),
('KATAKANA', 'ポ', 'po');

-- ============================================================
-- KATAKANA — combinaisons yōon
-- ============================================================
INSERT INTO script (sc_type, sc_character, sc_romaji) VALUES
('KATAKANA', 'キャ', 'kya'),
('KATAKANA', 'キュ', 'kyu'),
('KATAKANA', 'キョ', 'kyo'),

('KATAKANA', 'シャ', 'sha'),
('KATAKANA', 'シュ', 'shu'),
('KATAKANA', 'ショ', 'sho'),

('KATAKANA', 'チャ', 'cha'),
('KATAKANA', 'チュ', 'chu'),
('KATAKANA', 'チョ', 'cho'),

('KATAKANA', 'ニャ', 'nya'),
('KATAKANA', 'ニュ', 'nyu'),
('KATAKANA', 'ニョ', 'nyo'),

('KATAKANA', 'ヒャ', 'hya'),
('KATAKANA', 'ヒュ', 'hyu'),
('KATAKANA', 'ヒョ', 'hyo'),

('KATAKANA', 'ミャ', 'mya'),
('KATAKANA', 'ミュ', 'myu'),
('KATAKANA', 'ミョ', 'myo'),

('KATAKANA', 'リャ', 'rya'),
('KATAKANA', 'リュ', 'ryu'),
('KATAKANA', 'リョ', 'ryo'),

('KATAKANA', 'ギャ', 'gya'),
('KATAKANA', 'ギュ', 'gyu'),
('KATAKANA', 'ギョ', 'gyo'),

('KATAKANA', 'ジャ', 'ja'),
('KATAKANA', 'ジュ', 'ju'),
('KATAKANA', 'ジョ', 'jo'),

('KATAKANA', 'ビャ', 'bya'),
('KATAKANA', 'ビュ', 'byu'),
('KATAKANA', 'ビョ', 'byo'),

('KATAKANA', 'ピャ', 'pya'),
('KATAKANA', 'ピュ', 'pyu'),
('KATAKANA', 'ピョ', 'pyo'),

-- Combinaisons étrangères (katakana uniquement — sons importés)
('KATAKANA', 'ファ', 'fa'),
('KATAKANA', 'フィ', 'fi'),
('KATAKANA', 'フェ', 'fe'),
('KATAKANA', 'フォ', 'fo'),
('KATAKANA', 'ウィ', 'wi'),
('KATAKANA', 'ウェ', 'we'),
('KATAKANA', 'ウォ', 'wo'),
('KATAKANA', 'ティ', 'ti'),
('KATAKANA', 'ディ', 'di'),
('KATAKANA', 'トゥ', 'tu'),
('KATAKANA', 'ドゥ', 'du'),
('KATAKANA', 'ヴァ', 'va'),
('KATAKANA', 'ヴィ', 'vi'),
('KATAKANA', 'ヴ',  'vu'),
('KATAKANA', 'ヴェ', 've'),
('KATAKANA', 'ヴォ', 'vo');