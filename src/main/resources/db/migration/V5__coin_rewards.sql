-- ─────────────────────────────────────────────────────────────
-- V5 — coin earn rate + per-business coin reward catalog
-- ─────────────────────────────────────────────────────────────

ALTER TABLE companies ADD COLUMN coin_rate DOUBLE PRECISION;

CREATE TABLE coin_rewards (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID         NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title       VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    coin_cost   INTEGER      NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_coin_cost CHECK (coin_cost >= 1)
);
CREATE INDEX idx_coin_rewards_company ON coin_rewards (company_id);
CREATE INDEX idx_coin_rewards_active  ON coin_rewards (active);
