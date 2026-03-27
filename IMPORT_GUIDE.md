# KotobaLearn — Guide d'import des données

Ce guide décrit dans quel ordre effectuer les imports et enrichissements
pour peupler la base de données sur une nouvelle machine.

---

## Prérequis

- PostgreSQL lancé, base `kotobalearn` créée
- Backend Spring Boot lancé (`mvn spring-boot:run`)
  → Flyway applique automatiquement les migrations V1-V12 au démarrage
- Les fichiers de données ci-dessous placés dans `backend/src/main/resources/data/`

---

## Fichiers de données requis

| Fichier | Source | Licence | À placer dans |
|---------|--------|---------|---------------|
| `ka_data.csv` | KanjiAlive | CC BY 4.0 | `data/` |
| `japanese-radicals.csv` | KanjiAlive | CC BY 4.0 | `data/` |
| `kanjidic2.xml` | EDRDG | CC BY-SA 4.0 | `data/` |
| `JMdict_e_examp.xml` | EDRDG | CC BY-SA 4.0 | `data/` |
| `kradfile` | EDRDG | CC BY-SA 4.0 | `data/` |
| `kradfile2` | EDRDG | CC BY-SA 4.0 | `data/` |
| `radkfile` | EDRDG | CC BY-SA 4.0 | `data/` |
| `radkfile2` | EDRDG | CC BY-SA 4.0 | `data/` |
| `kanji-data.json` | davidluzgouveia (MIT) | MIT | `data/` |
| `jlpt/original_data/n1.csv` | stephenmk (CC BY-SA) | CC BY-SA 4.0 | `data/jlpt/original_data/` |
| `jlpt/original_data/n2.csv` | stephenmk (CC BY-SA) | CC BY-SA 4.0 | `data/jlpt/original_data/` |
| `jlpt/original_data/n3.csv` | stephenmk (CC BY-SA) | CC BY-SA 4.0 | `data/jlpt/original_data/` |
| `jlpt/original_data/n4.csv` | stephenmk (CC BY-SA) | CC BY-SA 4.0 | `data/jlpt/original_data/` |
| `jlpt/original_data/n5.csv` | stephenmk (CC BY-SA) | CC BY-SA 4.0 | `data/jlpt/original_data/` |

### Télécharger les fichiers manquants

```bash
# kanji-data.json (JLPT kanji, davidluzgouveia, MIT)
curl -o data/kanji-data.json \
  https://raw.githubusercontent.com/davidluzgouveia/kanji-data/master/kanji.json

# kradfile + kradfile2 (EDRDG)
# https://www.edrdg.org/kradfile/

# radkfile + radkfile2 (EDRDG)
# https://www.edrdg.org/krad/kradinf.html

# jlpt vocab CSVs (stephenmk)
# https://github.com/stephenmk/yomitan-jlpt-vocab
```

---

## Ordre des imports

> ⚠️ **Respecter l'ordre** — certains imports dépendent des données
> insérées par les imports précédents.

---

### ÉTAPE 1 — Imports de base kanji

```bash
# 1a. KanjiAlive : radicaux + 1235 kanji avec médias (vidéos, audio)
#     Insère les radicaux dans `radical` et les kanji dans `kanji`
#     avec leurs lectures on/kun et exemples audio
curl -X POST http://localhost:8080/api/admin/import/kanjialive
```

```bash
# 1b. Kanjidic2 : complète les 10 384 kanji
#     Ajoute les kanji manquants, enrichit strokes, grade, kodansha, nelson
#     Prérequis : 1a terminé (pour ne pas dupliquer les kanji KanjiAlive)
curl -X POST http://localhost:8080/api/admin/import/kanjidic
```

---

### ÉTAPE 2 — Import vocabulaire

```bash
# 2a. JMdict : ~215 000 mots japonais avec lectures et traductions anglaises
#     Peuple les tables `word`, `word_kanji`, `tag`, `word_tag`
curl -X POST http://localhost:8080/api/admin/import/jmdict
```

---

### ÉTAPE 3 — Enrichissements mots

```bash
# 3a. JMdict seq + fréquence : extrait word_jmdict_seq et word_frequency_rank
#     word_frequency_rank : 1=très courant (ichi1/news1), 2=courant, ...
#     Prérequis : 2a terminé
curl -X POST http://localhost:8080/api/admin/enrich/jmdict-seq
```

```bash
# 3b. JLPT vocabulaire : assigne les niveaux JLPT (N1-N5) aux MOTS
#     Source : stephenmk/yomitan-jlpt-vocab (basé sur Jitendex)
#     Prérequis : 3a terminé (utilise word_jmdict_seq pour le matching)
curl -X POST http://localhost:8080/api/admin/enrich/jlpt-vocab
```

---

### ÉTAPE 4 — Enrichissements JLPT kanji

```bash
# 4a. JLPT kanji + grades scolaires : mise à jour depuis kanji-data.json
#     Source : davidluzgouveia/kanji-data (MIT)
#     Utilise jlpt_new (liste communautaire post-2010, N1 complet ~1000 kanji)
#     Grades : 1-6=primaire, 8=secondaire (joyo), null=hors programme
#     ⚠️ REMPLACE les niveaux JLPT kanji de Kanjidic2 (incomplets pour N1)
#     Prérequis : 1b terminé
curl -X POST http://localhost:8080/api/admin/enrich/kanji-jlpt
```

---

### ÉTAPE 5 — Radicaux et composants

```bash
# 5a. KRADFILE : remplit kanji_component avec les composants visuels
#     Insère les composants manquants dans `radical` (rad_type='component')
#     Exemple : 語 → [言, 五, 口]
#     Source : KRADFILE + KRADFILE2 (EDRDG, CC BY-SA, encodage EUC-JP)
#     Prérequis : 1a et 1b terminés
curl -X POST http://localhost:8080/api/admin/enrich/kradfile
```

```bash
# 5b. RADKFILE strokes : met à jour rad_strokes sur les composants
#     Extrait le nombre de traits depuis les lignes "$ 一 1" du RADKFILE
#     Prérequis : 5a terminé
curl -X POST http://localhost:8080/api/admin/enrich/radkfile-strokes
```

```bash
# 5c. Noms des radicaux : enrichit les composants KRADFILE avec les noms
#     et sens des radicaux KanjiAlive (via table de correspondance Kangxi↔CJK)
#     Résultat : les composants comme 口, 言, 水 ont maintenant leur nom romaji
#     Prérequis : 5a + 5b terminés
curl -X POST http://localhost:8080/api/admin/enrich/radical-names
```

---

## Récapitulatif complet (copier-coller)

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

Après l'import complet, ces chiffres devraient être cohérents :

```sql
-- Kanji
SELECT COUNT(*) FROM kanji;                                      -- ~10 384
SELECT COUNT(*) FROM kanji WHERE jlpt_id IS NOT NULL;           -- ~2 200 (N1-N5)
SELECT jl.jlpt_code, COUNT(*) FROM kanji k
  JOIN jlpt_level jl ON jl.jlpt_id = k.jlpt_id
  GROUP BY jl.jlpt_code ORDER BY jl.jlpt_code;
-- N1: ~1015, N2: ~366, N3: ~361, N4: ~166, N5: ~80

SELECT COUNT(*) FROM kanji WHERE kanji_grade IS NOT NULL;        -- ~2 136
SELECT kanji_grade, COUNT(*) FROM kanji
  GROUP BY kanji_grade ORDER BY kanji_grade;

-- Vocabulaire
SELECT COUNT(*) FROM word;                                       -- ~215 000
SELECT COUNT(*) FROM word WHERE word_jlpt_id IS NOT NULL;       -- ~8 688

-- Radicaux et composants
SELECT COUNT(*) FROM radical WHERE rad_type = 'component';       -- ~250
SELECT COUNT(*) FROM kanji_component;                            -- ~43 732
SELECT COUNT(*) FROM radical WHERE rad_name_romaji IS NOT NULL
  AND rad_type = 'component';                                    -- composants nommés
```

---

## Migrations Flyway appliquées automatiquement

| Version | Description |
|---------|-------------|
| V1 | Schéma initial (kanji, radical, reading, example, word, script, jlpt_level) |
| V2 | read_romaji nullable |
| V3 | Hiragana et Katakana dans `script` |
| V4 | Table `word_example` (exemples Tatoeba) |
| V5 | Tables `tag` et `word_tag` |
| V6 | Colonnes `word_jmdict_seq` et `word_frequency_rank` |
| V7 | Index sur `word_japanese` |
| V8 | Index pour import JLPT mots |
| V9 | Table `kanji_component` (composants visuels multi-radical) |
| V10 | Reset radicaux + colonne `rad_type` |
| V11 | Contrainte UNIQUE sur `radical.rad_character` |
| V12 | `rad_strokes` nullable |

---

## Notes importantes

- Les fichiers `kradfile` et `radkfile` sont encodés en **EUC-JP** — ne pas les ouvrir avec un éditeur qui les convertirait en UTF-8 avant import.
- `kanji-data.json` utilise le champ `jlpt_new` (liste post-2010) et non `jlpt_old` (ancienne liste officielle). `jlpt_new` donne une meilleure couverture de N1.
- Il n'existe pas de grade 7 dans le système japonais — c'est normal.
- Les imports sont **idempotents** (relançables sans duplication) grâce aux vérifications `findByKanjiCharacter`, `ON CONFLICT DO NOTHING`, etc.