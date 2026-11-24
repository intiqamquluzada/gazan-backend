-- Push notification preferences (the app's Notifications screen). The
-- in-app inbox still records everything; these only gate device pushes.
ALTER TABLE users ADD COLUMN notify_rewards   BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN notify_offers    BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN notify_weekly    BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN notify_marketing BOOLEAN NOT NULL DEFAULT FALSE;

-- False for accounts created through Google / Apple: their stored hash is
-- random, so they set a password without typing a "current" one.
ALTER TABLE users ADD COLUMN password_set BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE users SET password_set = FALSE
 WHERE google_sub IS NOT NULL OR apple_sub IS NOT NULL;
