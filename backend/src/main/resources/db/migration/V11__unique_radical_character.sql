-- V11 : Contrainte UNIQUE sur radical.rad_character
-- Nécessaire pour le ON CONFLICT dans KradfileImportService

ALTER TABLE radical ADD CONSTRAINT uq_radical_character UNIQUE (rad_character);