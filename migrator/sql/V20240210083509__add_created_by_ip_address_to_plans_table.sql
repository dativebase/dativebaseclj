ALTER TABLE plans
  ADD COLUMN created_by_ip_address text NOT NULL DEFAULT 'unknown';

CREATE INDEX plans_created_by_ip_address_created_at_idx
  ON plans
  USING btree (created_by_ip_address, created_at);
