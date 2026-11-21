-- ─────────────────────────────────────────────────────────────
-- V25 — Users can report a review (App Store guideline 1.2 for
-- user-generated content). One report per user per review; admins see
-- the count in the moderation list and delete abusive reviews.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE review_reports (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id   UUID          NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id     UUID          NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    reason      VARCHAR(16)   NOT NULL,
    note        VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version     BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uq_review_report UNIQUE (review_id, user_id),
    CONSTRAINT chk_review_report_reason CHECK (reason IN ('OTHER', 'SPAM', 'ABUSIVE', 'FAKE'))
);
CREATE INDEX idx_review_reports_review ON review_reports (review_id);
