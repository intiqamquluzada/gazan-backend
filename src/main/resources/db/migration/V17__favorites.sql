-- ─────────────────────────────────────────────────────────────
-- V15 — Favorite: a customer's saved/bookmarked businesses.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE favorites (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    company_id  UUID        NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uq_favorite UNIQUE (user_id, company_id)
);
CREATE INDEX idx_favorites_user ON favorites (user_id);
