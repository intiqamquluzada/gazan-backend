-- ─────────────────────────────────────────────────────────────
-- V1 — initial schema
-- Users + refresh tokens. Everything else (companies, programs,
-- cards, events) lives in subsequent migrations alongside its
-- feature module.
-- ─────────────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ───────────── users ────────────────────────────────────────
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(32),
    avatar_url      VARCHAR(512),
    business_name   VARCHAR(120),
    role            VARCHAR(32)  NOT NULL,
    locale          VARCHAR(8)   NOT NULL DEFAULT 'AZ',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_users_role
        CHECK (role IN ('CUSTOMER', 'BUSINESS_OWNER', 'ADMIN')),
    CONSTRAINT chk_users_locale
        CHECK (locale IN ('AZ', 'EN', 'RU', 'TR'))
);

CREATE UNIQUE INDEX idx_users_email ON users (LOWER(email));
CREATE INDEX        idx_users_role  ON users (role);

-- ───────────── refresh_tokens ───────────────────────────────
CREATE TABLE refresh_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    token_value  VARCHAR(128) NOT NULL,
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    user_agent   VARCHAR(256),
    ip           VARCHAR(64),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_refresh_token_value ON refresh_tokens (token_value);
CREATE INDEX        idx_refresh_user        ON refresh_tokens (user_id);
CREATE INDEX        idx_refresh_active      ON refresh_tokens (user_id, expires_at)
                    WHERE revoked_at IS NULL;
