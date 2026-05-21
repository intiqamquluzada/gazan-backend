-- ─────────────────────────────────────────────────────────────
-- V9 — RewardClaim: a unified "I have this gift" voucher.
-- Created either by a coin purchase (status=ACTIVE) or by a card
-- redemption at the cashier (status=USED, historical record).
-- ─────────────────────────────────────────────────────────────

CREATE TABLE reward_claims (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    company_id  UUID         NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    source      VARCHAR(16)  NOT NULL,
    ref_id      UUID,
    title       VARCHAR(160) NOT NULL,
    coin_cost   INTEGER      NOT NULL DEFAULT 0,
    status      VARCHAR(16)  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_reward_source CHECK (source IN ('LOYALTY_CARD', 'COIN')),
    CONSTRAINT chk_reward_status CHECK (status IN ('ACTIVE', 'USED'))
);
CREATE INDEX idx_reward_user_status         ON reward_claims (user_id, status);
CREATE INDEX idx_reward_user_company_status ON reward_claims (user_id, company_id, status);
