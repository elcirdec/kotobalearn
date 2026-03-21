-- V12 : rad_strokes devient nullable
-- Les composants KRADFILE n'ont pas de nombre de traits défini

ALTER TABLE radical ALTER COLUMN rad_strokes DROP NOT NULL;