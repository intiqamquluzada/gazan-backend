-- ─────────────────────────────────────────────────────────────
-- V21 — NFC cards. A venue gets one physical card per action:
--   * STAMP  → a card bound to ONE loyalty program (campaign)
--   * POINTS → a card that credits a fixed number of coins
-- The cashier taps the chosen card on the customer's phone; the phone
-- reads the token and posts it to /api/v1/nfc/tap.
--
-- NTAG 213 tags carry a STATIC token, so replay protection lives on the
-- server: cooldown, daily caps, geofence, UID binding and tap auditing.
-- When tags are swapped for NTAG 424 DNA, `key_version`/`last_counter`
-- carry the dynamic (SUN/CMAC) state without another migration.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE nfc_tags (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id         UUID         NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    program_id         UUID         REFERENCES loyalty_programs(id) ON DELETE SET NULL,
    kind               VARCHAR(16)  NOT NULL,            -- STAMP | POINTS
    label              VARCHAR(80),
    token              VARCHAR(64)  NOT NULL UNIQUE,     -- random secret written on the tag
    tag_uid            VARCHAR(32),                      -- learned on first tap, then enforced
    key_version        INTEGER      NOT NULL DEFAULT 1,
    last_counter       BIGINT       NOT NULL DEFAULT 0,  -- NTAG 424 DNA read counter (0 = static tag)
    points_value       INTEGER      NOT NULL DEFAULT 100,
    stamps_value       INTEGER      NOT NULL DEFAULT 1,
    cooldown_sec       INTEGER      NOT NULL DEFAULT 300,
    daily_cap_per_user INTEGER      NOT NULL DEFAULT 3,
    status             VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | LOST | REVOKED | SUSPENDED
    provisioned_at     TIMESTAMPTZ,
    revoked_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_nfc_kind   CHECK (kind IN ('STAMP', 'POINTS')),
    CONSTRAINT chk_nfc_status CHECK (status IN ('ACTIVE', 'LOST', 'REVOKED', 'SUSPENDED')),
    -- a stamp card must point at the campaign it stamps
    CONSTRAINT chk_nfc_program CHECK (kind <> 'STAMP' OR program_id IS NOT NULL)
);
CREATE INDEX idx_nfc_tags_company ON nfc_tags (company_id);

-- Every tap, accepted or not: the audit trail and the fraud signal source.
CREATE TABLE nfc_taps (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_id           UUID         REFERENCES nfc_tags(id) ON DELETE SET NULL,
    user_id          UUID         REFERENCES users(id) ON DELETE SET NULL,
    company_id       UUID         REFERENCES companies(id) ON DELETE SET NULL,
    counter          BIGINT       NOT NULL DEFAULT 0,
    result           VARCHAR(24)  NOT NULL,   -- OK | REPLAY | COOLDOWN | DAILY_CAP | ...
    points_delta     INTEGER      NOT NULL DEFAULT 0,
    stamps_delta     INTEGER      NOT NULL DEFAULT 0,
    distance_m       INTEGER,
    device_id        VARCHAR(80),
    client_tap_id    VARCHAR(64),
    suspicious       BOOLEAN      NOT NULL DEFAULT FALSE,
    note             VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_nfc_taps_tag    ON nfc_taps (tag_id, created_at DESC);
CREATE INDEX idx_nfc_taps_user   ON nfc_taps (user_id, created_at DESC);
CREATE INDEX idx_nfc_taps_recent ON nfc_taps (created_at DESC);

-- Same tap submitted twice (retry / offline queue) must not pay twice.
CREATE UNIQUE INDEX uq_nfc_taps_client
    ON nfc_taps (user_id, client_tap_id) WHERE client_tap_id IS NOT NULL;
-- Dynamic tags: a counter value can only ever be used once.
CREATE UNIQUE INDEX uq_nfc_taps_counter
    ON nfc_taps (tag_id, counter) WHERE counter > 0;

-- Per-venue switch: the old QR flow stays available.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS loyalty_mode VARCHAR(8) NOT NULL DEFAULT 'BOTH';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS nfc_geofence_m INTEGER NOT NULL DEFAULT 300;
