-- ─────────────────────────────────────────────────────────────
-- V4 — owner-editable company profile fields
-- ─────────────────────────────────────────────────────────────

ALTER TABLE companies ADD COLUMN phone         VARCHAR(32);
ALTER TABLE companies ADD COLUMN instagram     VARCHAR(64);
ALTER TABLE companies ADD COLUMN working_hours VARCHAR(120);
ALTER TABLE companies ADD COLUMN latitude      DOUBLE PRECISION;
ALTER TABLE companies ADD COLUMN longitude     DOUBLE PRECISION;
ALTER TABLE companies ADD COLUMN amenities     VARCHAR(255);
ALTER TABLE companies ADD COLUMN photo_urls    VARCHAR(2000);
ALTER TABLE companies ADD COLUMN menu_url      VARCHAR(512);
