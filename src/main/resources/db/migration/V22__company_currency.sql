-- ─────────────────────────────────────────────────────────────
-- V22 — the app now serves Azerbaijan AND Türkiye, so prices must
-- carry their own currency instead of assuming manat.
-- AZN (₼) stays the default for existing venues.
-- ─────────────────────────────────────────────────────────────
ALTER TABLE companies ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'AZN';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS country VARCHAR(2) NOT NULL DEFAULT 'AZ';
