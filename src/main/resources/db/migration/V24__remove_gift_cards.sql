-- ─────────────────────────────────────────────────────────────
-- V24 — the GIFT card kind is retired. Gifts are earned through stamp
-- campaigns or bought with coins; a card that handed one out directly is
-- gone. Existing GIFT cards fall back to UNASSIGNED so the owner can give
-- them a new job (stamp, points or spend). Vouchers they already issued
-- stay in reward_claims and remain redeemable.
-- ─────────────────────────────────────────────────────────────

UPDATE nfc_tags SET kind = 'UNASSIGNED' WHERE kind = 'GIFT';

ALTER TABLE nfc_tags DROP CONSTRAINT IF EXISTS chk_nfc_kind;
ALTER TABLE nfc_tags ADD CONSTRAINT chk_nfc_kind
    CHECK (kind IN ('UNASSIGNED', 'STAMP', 'POINTS', 'SPEND'));

DROP INDEX IF EXISTS idx_nfc_tags_reward;
ALTER TABLE nfc_tags DROP COLUMN IF EXISTS coin_reward_id;
