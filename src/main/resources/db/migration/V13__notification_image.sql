-- ─────────────────────────────────────────────────────────────
-- V13 — optional cover image on notifications. Stores the relative
-- URL produced by /api/v1/images upload (e.g. /api/v1/images/{id}).
-- ─────────────────────────────────────────────────────────────

ALTER TABLE notifications
    ADD COLUMN image_url VARCHAR(512);
