-- Web admin panel sign-in: authenticator app (TOTP) second factor and
-- cookie-held refresh tokens that are kept apart from the mobile app's.

-- AES-GCM encrypted base32 secret. Set on the first password step;
-- totp_enabled_at marks the moment the first code was confirmed.
ALTER TABLE users ADD COLUMN totp_secret     VARCHAR(255);
ALTER TABLE users ADD COLUMN totp_enabled_at TIMESTAMPTZ;
-- Last 30-second step accepted, so a code can't be used twice.
ALTER TABLE users ADD COLUMN totp_last_step  BIGINT;

-- APP: mobile app (rotated via /auth/refresh). ADMIN_WEB: web admin panel
-- (rotated only via /admin/auth/refresh, short idle lifetime).
ALTER TABLE refresh_tokens ADD COLUMN channel VARCHAR(16) NOT NULL DEFAULT 'APP';
