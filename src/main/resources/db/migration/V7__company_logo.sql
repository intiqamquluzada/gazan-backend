-- ─────────────────────────────────────────────────────────────
-- V7 — business profile picture / logo (uploaded image URL).
-- Nullable & additive; falls back to the brand monogram when null.
-- ─────────────────────────────────────────────────────────────

ALTER TABLE companies ADD COLUMN logo_url VARCHAR(512);
