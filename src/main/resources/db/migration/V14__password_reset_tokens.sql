-- ─────────────────────────────────────────────────────────────
-- V14 — PasswordResetToken: a short-lived 6-digit code emailed to
-- the user. The code is stored hashed (never plaintext), single-use,
-- and expires after a few minutes.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE password_reset_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash   VARCHAR(100) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_reset_user ON password_reset_tokens (user_id);
