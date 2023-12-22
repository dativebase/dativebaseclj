ALTER TABLE forms
  ADD COLUMN updated_by uuid NOT NULL;

ALTER TABLE forms
  ADD CONSTRAINT fk_forms_updated_by_user_id FOREIGN KEY (updated_by) REFERENCES users(id);

DROP TABLE machine_users;

ALTER TABLE users
  ADD COLUMN created_by uuid;

ALTER TABLE users
  ADD COLUMN updated_by uuid;

ALTER TABLE users
  ADD CONSTRAINT fk_users_created_by_user_id FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE users
  ADD CONSTRAINT fk_users_updated_by_user_id FOREIGN KEY (updated_by) REFERENCES users(id);

ALTER TABLE users
  ADD COLUMN inserted_at timestamp with time zone DEFAULT now() NOT NULL;

ALTER TABLE olds
  ADD COLUMN created_by uuid;

ALTER TABLE olds
  ADD COLUMN updated_by uuid;

ALTER TABLE olds
  ADD CONSTRAINT fk_olds_created_by_user_id FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE olds
  ADD CONSTRAINT fk_olds_updated_by_user_id FOREIGN KEY (updated_by) REFERENCES users(id);

ALTER TABLE olds
  ADD COLUMN inserted_at timestamp with time zone DEFAULT now() NOT NULL;

ALTER TABLE users_olds
  ADD COLUMN created_by uuid;

ALTER TABLE users_olds
  ADD COLUMN updated_by uuid;

ALTER TABLE users_olds
  ADD CONSTRAINT fk_users_olds_created_by_user_id FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE users_olds
  ADD CONSTRAINT fk_users_olds_updated_by_user_id FOREIGN KEY (updated_by) REFERENCES users(id);

ALTER TABLE users_olds
  ADD COLUMN inserted_at timestamp with time zone DEFAULT now() NOT NULL;
