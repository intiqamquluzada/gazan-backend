-- ─────────────────────────────────────────────────────────────
-- V2 — companies, loyalty (programs/cards/events), promotions
-- ─────────────────────────────────────────────────────────────

-- ───────── companies ────────────────────────────────────────
CREATE TABLE companies (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(120) NOT NULL,
    tagline         VARCHAR(240),
    category        VARCHAR(32)  NOT NULL,
    logo_emoji      VARCHAR(8),
    cover_color_hex BIGINT       NOT NULL DEFAULT 0,
    address         VARCHAR(240),
    rating          DOUBLE PRECISION,
    review_count    INTEGER      NOT NULL DEFAULT 0,
    is_featured     BOOLEAN      NOT NULL DEFAULT FALSE,
    owner_id        UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_companies_category CHECK (category IN
        ('COFFEE','RESTAURANT','BEAUTY','BARBER','CARWASH','FITNESS','BAKERY','OTHER'))
);
CREATE INDEX idx_companies_category ON companies (category);
CREATE INDEX idx_companies_owner    ON companies (owner_id);
CREATE INDEX idx_companies_featured ON companies (is_featured);


-- ───────── loyalty programs ─────────────────────────────────
CREATE TABLE loyalty_programs (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id       UUID          NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title            VARCHAR(120)  NOT NULL,
    description      VARCHAR(500),
    stamps_required  INTEGER       NOT NULL,
    reward_type      VARCHAR(32)   NOT NULL,
    reward_value     NUMERIC(10,2),
    reward_item      VARCHAR(80),
    expires_at       TIMESTAMPTZ,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT chk_lp_stamps CHECK (stamps_required BETWEEN 1 AND 50),
    CONSTRAINT chk_lp_type CHECK (reward_type IN
        ('FREE_ITEM','PERCENTAGE_DISCOUNT','FIXED_DISCOUNT','CASHBACK'))
);
CREATE INDEX idx_lp_company ON loyalty_programs (company_id);
CREATE INDEX idx_lp_active  ON loyalty_programs (active);


-- ───────── loyalty cards ────────────────────────────────────
CREATE TABLE loyalty_cards (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    program_id             UUID         NOT NULL REFERENCES loyalty_programs(id) ON DELETE CASCADE,
    company_id             UUID         NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    stamps                 INTEGER      NOT NULL DEFAULT 0,
    stamps_required        INTEGER      NOT NULL,
    rewards_available      INTEGER      NOT NULL DEFAULT 0,
    total_rewards_claimed  INTEGER      NOT NULL DEFAULT 0,
    last_activity_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_card_user_program UNIQUE (user_id, program_id)
);
CREATE INDEX idx_card_user    ON loyalty_cards (user_id);
CREATE INDEX idx_card_company ON loyalty_cards (company_id);


-- ───────── loyalty events ───────────────────────────────────
CREATE TABLE loyalty_events (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id     UUID         NOT NULL REFERENCES loyalty_cards(id) ON DELETE CASCADE,
    type        VARCHAR(32)  NOT NULL,
    amount      INTEGER      NOT NULL DEFAULT 1,
    note        VARCHAR(240),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_event_type CHECK (type IN ('STAMP_ADDED','REWARD_CLAIMED'))
);
CREATE INDEX idx_event_card    ON loyalty_events (card_id);
CREATE INDEX idx_event_created ON loyalty_events (created_at);


-- ───────── stories ──────────────────────────────────────────
CREATE TABLE stories (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID         NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    headline            VARCHAR(120) NOT NULL,
    body                VARCHAR(240),
    emoji               VARCHAR(8),
    gradient_start_hex  BIGINT       NOT NULL DEFAULT 0,
    gradient_end_hex    BIGINT       NOT NULL DEFAULT 0,
    cta                 VARCHAR(40),
    duration_seconds    INTEGER      NOT NULL DEFAULT 5,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at          TIMESTAMPTZ,
    sort_order          INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_stories_company ON stories (company_id);
CREATE INDEX idx_stories_active  ON stories (active);


-- ───────── promotions ───────────────────────────────────────
CREATE TABLE promotions (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID         REFERENCES companies(id) ON DELETE SET NULL,
    tag                 VARCHAR(40)  NOT NULL,
    title               VARCHAR(120) NOT NULL,
    subtitle            VARCHAR(200),
    emoji               VARCHAR(8),
    gradient_start_hex  BIGINT       NOT NULL DEFAULT 0,
    gradient_end_hex    BIGINT       NOT NULL DEFAULT 0,
    cta                 VARCHAR(40),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    ends_at             TIMESTAMPTZ,
    sort_order          INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_promotions_active ON promotions (active);
CREATE INDEX idx_promotions_ends   ON promotions (ends_at);
