BEGIN;
ALTER TABLE users DROP CONSTRAINT users_email_key;
ALTER TABLE users
  ADD CONSTRAINT users_email_key
  UNIQUE NULLS NOT DISTINCT (email, destroyed_at);
COMMIT;
