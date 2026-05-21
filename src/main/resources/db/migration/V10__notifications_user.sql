-- ─────────────────────────────────────────────────────────────
-- V10 — per-user notifications. NULL user_id = broadcast to everyone
-- (existing behaviour). Set user_id = targeted notification (e.g.
-- "you used your reward").
-- ─────────────────────────────────────────────────────────────

ALTER TABLE notifications
    ADD COLUMN user_id UUID REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_notifications_user ON notifications (user_id);
