-- ─────────────────────────────────────────────────────────────
-- V27 — Email OTP for password sign-up. Before an account is created
-- the app proves the address: a 6-digit code is mailed, the user types
-- it back and gets a one-time ticket that /auth/register consumes.
-- Google / Apple sign-in does not use this table.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE email_verifications (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(254)  NOT NULL,
    code_hash    VARCHAR(100)  NOT NULL,
    expires_at   TIMESTAMPTZ   NOT NULL,
    attempts     INT           NOT NULL DEFAULT 0,
    verified_at  TIMESTAMPTZ,
    ticket_hash  VARCHAR(64),
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version      BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_email_verifications_email ON email_verifications (email, created_at DESC);
CREATE UNIQUE INDEX uq_email_verifications_ticket ON email_verifications (ticket_hash)
    WHERE ticket_hash IS NOT NULL;
