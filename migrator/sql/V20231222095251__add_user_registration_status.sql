ALTER TABLE users
  ADD COLUMN registration_status text NOT NULL DEFAULT 'pending';
