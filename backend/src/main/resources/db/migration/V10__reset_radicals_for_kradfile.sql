-- V10 : Reset des radicaux KanjiAlive et préparation pour KRADFILE
-- Les radicaux KanjiAlive sont remplacés par les composants KRADFILE (EDRDG)
-- qui couvrent 12 000+ kanji vs ~1235 pour KanjiAlive.

-- 1. Ajouter colonne rad_type pour distinguer composants et radicaux classiques
ALTER TABLE radical ADD COLUMN IF NOT EXISTS rad_type VARCHAR(20) DEFAULT 'component';

-- 2. Vider les liaisons existantes (ordre important pour respecter les FK)
UPDATE kanji SET rad_id = NULL;
DELETE FROM kanji_component;
DELETE FROM radical;