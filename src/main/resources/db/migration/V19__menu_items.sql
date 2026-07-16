-- ─────────────────────────────────────────────────────────────
-- V19 — structured menu. A business can EITHER paste a menu link
-- (companies.menu_url, already exists) OR build a menu here, grouped
-- by category. Both can coexist; the app prefers structured items
-- when present.
-- ─────────────────────────────────────────────────────────────

CREATE TABLE menu_items (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID          NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    category    VARCHAR(80)   NOT NULL,
    name        VARCHAR(160)  NOT NULL,
    description VARCHAR(400),
    price       NUMERIC(10,2),
    sort_order  INT           NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version     BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_menu_items_company ON menu_items (company_id, sort_order);
