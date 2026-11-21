-- ─────────────────────────────────────────────────────────────
-- V23 — phone-first and social sign-in, cashier staff accounts,
-- assignable NFC cards (gift/spend), the cashier spend window,
-- company approval, the admin audit log and missing indexes.
-- ─────────────────────────────────────────────────────────────

-- ───────── users ─────────────────────────────────────────────
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('CUSTOMER', 'BUSINESS_OWNER', 'CASHIER', 'ADMIN'));

ALTER TABLE users
    ADD COLUMN google_sub        VARCHAR(255),
    ADD COLUMN apple_sub         VARCHAR(255),
    ADD COLUMN phone_verified_at TIMESTAMPTZ,
    ADD COLUMN staff_company_id  UUID REFERENCES companies(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_users_google_sub ON users (google_sub) WHERE google_sub IS NOT NULL;
CREATE UNIQUE INDEX uq_users_apple_sub  ON users (apple_sub)  WHERE apple_sub  IS NOT NULL;
CREATE INDEX idx_users_staff_company    ON users (staff_company_id) WHERE staff_company_id IS NOT NULL;

-- Phones become '+' and digits only, so they can be matched at login.
UPDATE users
   SET phone = CASE
         WHEN regexp_replace(phone, '[^0-9]', '', 'g') = '' THEN NULL
         ELSE '+' || regexp_replace(regexp_replace(phone, '[^0-9]', '', 'g'), '^00', '')
       END
 WHERE phone IS NOT NULL;

-- Uniqueness is enforced by the application either way; the database
-- backs it up only when existing data allows it.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE phone IS NOT NULL
                GROUP BY phone HAVING count(*) > 1) THEN
        CREATE INDEX idx_users_phone ON users (phone) WHERE phone IS NOT NULL;
        RAISE NOTICE 'V23: duplicate phones exist, uq_users_phone not created';
    ELSE
        CREATE UNIQUE INDEX uq_users_phone ON users (phone) WHERE phone IS NOT NULL;
    END IF;
END $$;

-- ───────── password reset ────────────────────────────────────
ALTER TABLE password_reset_tokens
    ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0;

-- ───────── companies ─────────────────────────────────────────
-- Everything that exists today stays visible; new rows wait for an admin.
ALTER TABLE companies
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE companies ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE companies ADD CONSTRAINT chk_companies_status
    CHECK (status IN ('PENDING', 'APPROVED', 'SUSPENDED'));
CREATE INDEX idx_companies_status ON companies (status);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM companies WHERE owner_id IS NOT NULL
                    GROUP BY owner_id HAVING count(*) > 1) THEN
        CREATE UNIQUE INDEX uq_companies_owner ON companies (owner_id)
            WHERE owner_id IS NOT NULL;
    ELSE
        RAISE NOTICE 'V23: an owner has several companies, uq_companies_owner not created';
    END IF;
END $$;

-- ───────── NFC cards ─────────────────────────────────────────
ALTER TABLE nfc_tags DROP CONSTRAINT IF EXISTS chk_nfc_program;
ALTER TABLE nfc_tags DROP CONSTRAINT IF EXISTS chk_nfc_kind;
ALTER TABLE nfc_tags ADD CONSTRAINT chk_nfc_kind
    CHECK (kind IN ('UNASSIGNED', 'STAMP', 'POINTS', 'GIFT', 'SPEND'));
ALTER TABLE nfc_tags
    ADD COLUMN coin_reward_id UUID REFERENCES coin_rewards(id) ON DELETE SET NULL;

-- ───────── spend sessions (the cashier window) ───────────────
CREATE TABLE spend_sessions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID         NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id     UUID         NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    tag_id      UUID         REFERENCES nfc_tags(id)           ON DELETE SET NULL,
    status      VARCHAR(16)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    handled_by  UUID         REFERENCES users(id)              ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_spend_status
        CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED', 'EXPIRED'))
);
CREATE INDEX idx_spend_company_status ON spend_sessions (company_id, status, expires_at);
CREATE INDEX idx_spend_user           ON spend_sessions (user_id);
-- One open window per customer per venue.
CREATE UNIQUE INDEX uq_spend_open ON spend_sessions (company_id, user_id)
    WHERE status = 'OPEN';

-- ───────── admin audit log ───────────────────────────────────
CREATE TABLE admin_audit_log (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id     UUID          REFERENCES users(id) ON DELETE SET NULL,
    actor_email  VARCHAR(254),
    action       VARCHAR(48)   NOT NULL,
    target_type  VARCHAR(32),
    target_id    VARCHAR(64),
    detail       VARCHAR(500),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version      BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_audit_created ON admin_audit_log (created_at DESC);

-- ───────── missing lookup indexes ────────────────────────────
CREATE INDEX IF NOT EXISTS idx_reward_company_status ON reward_claims (company_id, status);
CREATE INDEX IF NOT EXISTS idx_cards_program         ON loyalty_cards (program_id);
CREATE INDEX IF NOT EXISTS idx_coin_company          ON coin_transactions (company_id);
CREATE INDEX IF NOT EXISTS idx_favorites_company     ON favorites (company_id);
CREATE INDEX IF NOT EXISTS idx_nfc_tags_program      ON nfc_tags (program_id);
CREATE INDEX IF NOT EXISTS idx_nfc_tags_reward       ON nfc_tags (coin_reward_id);
CREATE INDEX IF NOT EXISTS idx_nfc_taps_company      ON nfc_taps (company_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_submitted_by    ON notifications (submitted_by);
CREATE INDEX IF NOT EXISTS idx_notif_approved_by     ON notifications (approved_by);
