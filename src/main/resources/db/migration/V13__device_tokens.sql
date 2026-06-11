-- ─────────────────────────────────────────────────────────────
-- V13 — DeviceToken: per-device push tokens (FCM/APNs) so the
-- backend can deliver real phone notifications, not just the in-app
-- inbox. One row per device; a token is globally unique and gets
-- re-pointed to its latest owner on re-registration.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE device_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL,
    platform    VARCHAR(16)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_device_token UNIQUE (token),
    CONSTRAINT chk_device_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);
CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);
