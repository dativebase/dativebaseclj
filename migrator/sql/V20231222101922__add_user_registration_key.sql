ALTER TABLE users
  ADD COLUMN registration_key uuid DEFAULT public.uuid_generate_v4() NOT NULL;
