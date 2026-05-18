-- ─────────────────────────────────────────────────────────────
-- V3 — coin wallet (signed ledger; balance = SUM(amount))
-- ─────────────────────────────────────────────────────────────

CREATE TABLE coin_transactions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    company_id  UUID                  REFERENCES companies(id) ON DELETE SET NULL,
    amount      INTEGER      NOT NULL,
    type        VARCHAR(16)  NOT NULL,
    note        VARCHAR(240),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_coin_type CHECK (type IN ('EARN','SPEND'))
);
CREATE INDEX idx_coin_user         ON coin_transactions (user_id);
CREATE INDEX idx_coin_user_company ON coin_transactions (user_id, company_id);
CREATE INDEX idx_coin_created      ON coin_transactions (created_at);
