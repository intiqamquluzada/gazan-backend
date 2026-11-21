-- Sign in with Apple: refresh token kept so account deletion can revoke it
-- (App Store guideline 5.1.1(v)).
ALTER TABLE users ADD COLUMN apple_refresh_token VARCHAR(512);
