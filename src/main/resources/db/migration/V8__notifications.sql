-- ─────────────────────────────────────────────────────────────
-- V8 — broadcast notifications + per-user read state.
-- One row per broadcast; unread = no read row for that (notif,user).
-- ─────────────────────────────────────────────────────────────

CREATE TABLE notifications (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(160)  NOT NULL,
    body        VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version     BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_notifications_created ON notifications (created_at);

CREATE TABLE notification_reads (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID        NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uq_notif_read UNIQUE (notification_id, user_id)
);
CREATE INDEX idx_notif_read_user ON notification_reads (user_id);
