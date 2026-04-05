# KotobaLearn — Guide d'import des données

Ce guide décrit dans quel ordre effectuer les imports et enrichissements
pour peupler la base de données sur une nouvelle machine.

---

## Prérequis

- PostgreSQL lancé, base `kotobalearn` créée
- Backend Spring Boot lancé (`mvn spring-boot:run`)
  → Flyway applique automatiquement les migrations V1-V12 au démarrage
- Les fichiers de données placés dans `backend/src/main/resources/data/`
  (voir `backend/src/main/resources/data/README-data.md`)

---

### Fichiers de données requis

| Fichier | Source | Licence | À placer dans |
|---------|--------|---------|---------------|
| `ka_data.csv` | KanjiAlive | CC BY 4.0 | `data/` |
| `japanese-radicals.csv` | KanjiAlive | CC BY 4.0 | `data/` |
| `kanjidic2.xml` | EDRDG | CC BY-SA 4.0 | `data/` |
| `JMdict_e_examp.xml` | EDRDG | CC BY-SA 4.0 | `data/` |
| `kradfile`, `kradfile2` | EDRDG | CC BY-SA 4.0 | `data/` |
| `radkfile`, `radkfile2` | EDRDG | CC BY-SA 4.0 | `data/` |
| `kanji-data.json` | davidluzgouveia | MIT | `data/` |
| `n1.csv` … `n5.csv` | stephenmk (yomitan‑jlpt‑vocab) | CC BY-SA 4.0 | `data/jlpt/original_data/` |
| **animCJK (SVGs)** | parsimonhi | Arphic Public License (kanji) + LGPL (kana) | `static/animcjk/` (clone git) |

---

## Étape 0 — SVGs animCJK (tracés animés)

```bash
# Cloner animCJK (~150 Mo)
git clone https://github.com/parsimonhi/animCJK.git

# Copier les kanji SVGs
mkdir -p backend/src/main/resources/static/animcjk/kanji
cp animCJK/svgsJa/*.svg backend/src/main/resources/static/animcjk/kanji/

# Copier les kana SVGs
mkdir -p backend/src/main/resources/static/animcjk/kana
cp animCJK/svgsJaKana/*.svg backend/src/main/resources/static/animcjk/kana/
```

Spring Boot sert ces fichiers automatiquement sur `http://localhost:8080/animcjk/...`

> **Licence** : les SVGs de kanji sont sous licence **Arphic Public License**, les SVGs de kana sous **LGPL** (compatible avec un usage commercial, crédit à mentionner).

---

## Étape 1 — Imports de base kanji

```bash
# 1a. KanjiAlive : radicaux + 1235 kanji (lectures, exemples audio)
curl -X POST http://localhost:8080/api/admin/import/kanjialive

# 1b. Kanjidic2 : complète les 10 384 kanji (strokes, grade, sens…)
curl -X POST http://localhost:8080/api/admin/import/kanjidic
```

---

## Étape 2 — Import vocabulaire

```bash
# JMdict : ~215 000 mots japonais avec lectures et traductions
curl -X POST http://localhost:8080/api/admin/import/jmdict
```

---

## Étape 3 — Enrichissements mots

```bash
# word_jmdict_seq + word_frequency_rank (fréquence d'usage)
curl -X POST http://localhost:8080/api/admin/enrich/jmdict-seq

# Niveaux JLPT des MOTS (N1-N5) — Source: stephenmk/yomitan-jlpt-vocab
curl -X POST http://localhost:8080/api/admin/enrich/jlpt-vocab
```

---

## Étape 4 — Enrichissements kanji

```bash
# Niveaux JLPT des KANJI + grades scolaires
# Source: davidluzgouveia/kanji-data (MIT), jlpt_new post-2010
# Résultat : ~2200 kanji avec JLPT (N1~1015, N2~366, N3~361, N4~166, N5~80)
curl -X POST http://localhost:8080/api/admin/enrich/kanji-jlpt
```

---

## Étape 5 — Radicaux et composants

```bash
# Composants visuels de chaque kanji (KRADFILE + KRADFILE2)
# Exemple : 語 → [言, 五, 口] dans kanji_component
curl -X POST http://localhost:8080/api/admin/enrich/kradfile

# Nombre de traits des composants (RADKFILE)
curl -X POST http://localhost:8080/api/admin/enrich/radkfile-strokes

# Noms des composants depuis KanjiAlive (japanese-radicals.csv)
curl -X POST http://localhost:8080/api/admin/enrich/radical-names
```

---

## Récapitulatif — copier-coller complet

```bash
# === IMPORT COMPLET KOTOBALEARN ===
# Durée estimée : 10-20 minutes selon la machine

# Étape 1 : Kanji de base
curl -X POST http://localhost:8080/api/admin/import/kanjialive
curl -X POST http://localhost:8080/api/admin/import/kanjidic

# Étape 2 : Vocabulaire
curl -X POST http://localhost:8080/api/admin/import/jmdict

# Étape 3 : Enrichissements mots
curl -X POST http://localhost:8080/api/admin/enrich/jmdict-seq
curl -X POST http://localhost:8080/api/admin/enrich/jlpt-vocab

# Étape 4 : JLPT kanji
curl -X POST http://localhost:8080/api/admin/enrich/kanji-jlpt

# Étape 5 : Radicaux
curl -X POST http://localhost:8080/api/admin/enrich/kradfile
curl -X POST http://localhost:8080/api/admin/enrich/radkfile-strokes
curl -X POST http://localhost:8080/api/admin/enrich/radical-names
```

---

## Vérifications en base

```sql
-- Kanji
SELECT COUNT(*) FROM kanji;                             -- ~10 384
SELECT jl.jlpt_code, COUNT(*)
  FROM kanji k JOIN jlpt_level jl ON jl.jlpt_id = k.jlpt_id
  GROUP BY jl.jlpt_code ORDER BY jl.jlpt_code;
-- N1: ~1015  N2: ~366  N3: ~361  N4: ~166  N5: ~80

SELECT kanji_grade, COUNT(*) FROM kanji
  WHERE kanji_grade IS NOT NULL
  GROUP BY kanji_grade ORDER BY kanji_grade;
-- 1-6: primaire | 8: secondaire | 9: prénoms

-- Vocabulaire
SELECT COUNT(*) FROM word;                              -- ~215 000
SELECT COUNT(*) FROM word WHERE word_jlpt_id IS NOT NULL; -- ~8 688

-- Radicaux
SELECT COUNT(*) FROM kanji_component;                  -- ~43 732
SELECT COUNT(*) FROM radical WHERE rad_name_romaji IS NOT NULL
  AND rad_type = 'component';                          -- composants nommés
```

---

## Migrations Flyway (appliquées automatiquement)

| Version | Description |
|---------|-------------|
| V1 | Schéma initial |
| V2 | read_romaji nullable |
| V3 | Hiragana + Katakana |
| V4 | word_example (Tatoeba) |
| V5 | tag + word_tag |
| V6 | word_jmdict_seq + word_frequency_rank |
| V7 | Index word_japanese |
| V8 | Index JLPT import |
| V9 | kanji_component (multi-radical) |
| V10 | rad_type + reset radicaux |
| V11 | UNIQUE sur radical.rad_character |
| V12 | rad_strokes nullable |

---

## Notes

- `kradfile` / `radkfile` encodés en **EUC-JP** — ne pas convertir avant import
- `kanji-data.json` utilise `jlpt_new` (post-2010), pas `jlpt_old`
- Grade 7 n'existe pas dans le système japonais (1-6 primaire, 8 secondaire, 9 prénoms)
- Les SVGs animCJK ne sont pas dans le repo git (trop lourds) — voir `.gitignore`
- Les imports sont **idempotents** (relançables sans duplication)