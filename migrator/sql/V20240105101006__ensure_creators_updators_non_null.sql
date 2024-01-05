INSERT INTO users (first_name,
                   last_name,
                   email,
                   password)
  VALUES ('Root',
          'Root',
          'root',
          'root')
  ON CONFLICT DO NOTHING;

UPDATE olds
  SET created_by = (SELECT id FROM users WHERE email = 'root')
  WHERE created_by IS NULL;

UPDATE olds
  SET updated_by = (SELECT id FROM users WHERE email = 'root')
  WHERE updated_by IS NULL;

UPDATE users_olds
  SET created_by = (SELECT id FROM users WHERE email = 'root')
  WHERE created_by IS NULL;

UPDATE users_olds
  SET updated_by = (SELECT id FROM users WHERE email = 'root')
  WHERE updated_by IS NULL;

ALTER TABLE olds
  ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE olds
  ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE users_olds
  ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE users_olds
  ALTER COLUMN updated_by SET NOT NULL;
