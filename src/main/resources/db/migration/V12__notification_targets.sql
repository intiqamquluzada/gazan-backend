-- ─────────────────────────────────────────────────────────────
-- V12 — targeted notifications + business-owner approval flow.
--   target_type: BROADCAST (everyone), USER (one inbox via user_id),
--                COMPANY_CARDHOLDERS (every customer with a loyalty
--                card at target_company_id)
--   status:      APPROVED (visible to recipients), PENDING (waiting
--                for admin), REJECTED (kept for audit, not delivered)
--   submitted_by: the business owner who created the request — null
--                for admin-authored notifications which are
--                immediately APPROVED.
-- ─────────────────────────────────────────────────────────────

ALTER TABLE notifications
    ADD COLUMN target_type        VARCHAR(32)  NOT NULL DEFAULT 'BROADCAST',
    ADD COLUMN target_company_id  UUID         REFERENCES companies(id) ON DELETE CASCADE,
    ADD COLUMN status             VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN submitted_by       UUID         REFERENCES users(id)     ON DELETE SET NULL,
    ADD COLUMN approved_by        UUID         REFERENCES users(id)     ON DELETE SET NULL,
    ADD COLUMN approved_at        TIMESTAMPTZ;

-- Backfill: rows created before V12 belong to the old model. A row
-- with user_id NULL was a broadcast, otherwise it was direct-to-user.
UPDATE notifications
   SET target_type = CASE WHEN user_id IS NULL THEN 'BROADCAST' ELSE 'USER' END
 WHERE target_type = 'BROADCAST';

CREATE INDEX idx_notifications_status         ON notifications (status);
CREATE INDEX idx_notifications_target_company ON notifications (target_company_id);
CREATE INDEX idx_notifications_target_type    ON notifications (target_type);
