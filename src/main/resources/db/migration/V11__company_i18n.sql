-- Per-language translations for the most user-visible company fields.
-- The original `name` / `tagline` columns stay the Azerbaijani default —
-- so existing rows keep working and the API can return AZ when a
-- requested locale has no translation set.

ALTER TABLE companies
    ADD COLUMN name_en     VARCHAR(120),
    ADD COLUMN name_ru     VARCHAR(120),
    ADD COLUMN name_tr     VARCHAR(120),
    ADD COLUMN tagline_en  VARCHAR(240),
    ADD COLUMN tagline_ru  VARCHAR(240),
    ADD COLUMN tagline_tr  VARCHAR(240);
