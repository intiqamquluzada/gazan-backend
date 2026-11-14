-- ─────────────────────────────────────────────────────────────
-- V20 — optional background photo for Discover promotion banners.
-- NULL → the app falls back to the linked company's cover photo,
-- then to the gradient colours.
-- ─────────────────────────────────────────────────────────────
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS image_url VARCHAR(512);
