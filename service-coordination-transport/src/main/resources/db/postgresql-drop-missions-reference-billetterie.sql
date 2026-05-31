-- À exécuter UNE FOIS sur une base PostgreSQL existante qui contenait encore une colonne billetterie sur missions.
-- Couvre les anciens noms reference_g3 et reference_g1 (schéma simplifié : lien billetterie = missionId Kafka M-{id} uniquement).

ALTER TABLE missions DROP COLUMN IF EXISTS reference_g1;
ALTER TABLE missions DROP COLUMN IF EXISTS reference_g3;
