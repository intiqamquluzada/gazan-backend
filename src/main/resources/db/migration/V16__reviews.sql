-- ─────────────────────────────────────────────────────────────
-- V16 — Review: a customer's 1–5 star rating + optional comment for
-- a business. One review per (user, company); re-submitting updates it.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE reviews (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    company_id  UUID          NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    rating      INTEGER       NOT NULL,
    comment     VARCHAR(1000),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version     BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uq_review UNIQUE (user_id, company_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
);
CREATE INDEX idx_reviews_company ON reviews (company_id);
