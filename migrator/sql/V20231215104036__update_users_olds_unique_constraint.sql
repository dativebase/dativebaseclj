BEGIN;
ALTER TABLE users_olds DROP CONSTRAINT users_olds_unique;
ALTER TABLE users_olds
  ADD CONSTRAINT users_olds_unique
  UNIQUE NULLS NOT DISTINCT (user_id, old_slug, destroyed_at);
COMMIT;
