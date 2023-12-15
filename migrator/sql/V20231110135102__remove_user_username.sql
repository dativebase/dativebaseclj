ALTER TABLE users
  DROP COLUMN username;

ALTER TABLE users
  ADD UNIQUE (email);

CREATE INDEX users_email_idx ON users USING btree (email);
