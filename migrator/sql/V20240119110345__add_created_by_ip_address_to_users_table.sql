ALTER TABLE users
  ADD COLUMN created_by_ip_address text NOT NULL DEFAULT 'unknown';

CREATE INDEX created_by_ip_address_created_at_idx
  ON users
  USING btree (created_by_ip_address, created_at);
