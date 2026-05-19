-- ─────────────────────────────────────────────────────────────
-- V6 — uploaded images (stored in-DB so they survive container
--      rebuilds; the prod FS is ephemeral and there is no object
--      store). Served public-read via GET /api/v1/images/{id}.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE images (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    content_type VARCHAR(64)  NOT NULL,
    size_bytes   INTEGER      NOT NULL,
    bytes        BYTEA        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0
);
