-- :name create-user* :returning-execute-kebab
INSERT INTO users (first_name, last_name, email, password)
VALUES (:first-name, :last-name, :email, :password)
RETURNING id, first_name, last_name, email, password, created_at, updated_at, destroyed_at

-- :name update-user* :returning-execute-kebab
UPDATE users
SET first_name = :first-name,
    last_name = :last-name,
    email = :email,
    password = :password,
    updated_at = now()
WHERE id = :id::uuid
RETURNING id, first_name, last_name, email, password, created_at, updated_at, destroyed_at

-- :name get-user* :query :one-kebab
-- :doc Get a user by its id.
SELECT id, first_name, last_name, email, password, created_at, updated_at, destroyed_at
FROM users
WHERE id = :id::uuid
  AND destroyed_at IS NULL

-- :name get-user-by-email* :query :one-kebab
-- :doc Get a user by its email.
SELECT *
  FROM users
 WHERE email = :email
   AND destroyed_at IS NULL

-- :name get-user-with-roles* :query :many-kebab
-- :doc Get a user by its id, including all the roles on all the OLDs for that user.
SELECT u.id, u.first_name, u.last_name, u.email, u.password, uo.role, uo.old_slug, u.created_at, u.updated_at, u.destroyed_at
FROM users u
LEFT OUTER JOIN users_olds uo
  ON uo.user_id = u.id
    AND uo.destroyed_at IS NULL
WHERE u.id = :id::uuid
  AND u.destroyed_at IS NULL

-- :name delete-user* :returning-execute-kebab
UPDATE users
SET destroyed_at = now(),
    updated_at = now()
WHERE id = :id::uuid
RETURNING id, first_name, last_name, email, password, created_at, updated_at, destroyed_at

-- :name create-user-old* :returning-execute-kebab
INSERT INTO users_olds (user_id, old_slug, role)
VALUES (:user-id, :old-slug, :role)
RETURNING id, user_id, old_slug, role, created_at, updated_at, destroyed_at

-- :name update-user-old* :returning-execute-kebab
UPDATE users_olds
SET role = :role,
    updated_at = now()
WHERE id = :id::uuid
RETURNING id, user_id, old_slug, role, created_at, updated_at, destroyed_at

-- :name delete-user-old* :returning-execute-kebab
UPDATE users_olds
SET destroyed_at = now(),
    updated_at = now()
WHERE id = :id::uuid
RETURNING id, user_id, old_slug, role, created_at, updated_at, destroyed_at

-- :name create-machine-user* :returning-execute-kebab
INSERT INTO machine_users (user_id, api_key)
VALUES (:user-id, :api-key)
RETURNING id, user_id, api_key, created_at, updated_at, destroyed_at

-- :name delete-machine-user* :returning-execute-kebab
UPDATE machine_users
SET destroyed_at = now(),
    updated_at = now()
WHERE id = :id::uuid
RETURNING id, user_id, api_key, created_at, updated_at, destroyed_at

-- :name get-machine-users-for-user* :query :many-kebab
-- :doc Get the active machine users for a given user.
SELECT id, api_key
FROM machine_users
WHERE user_id = :user-id::uuid
  AND destroyed_at IS NULL

-- :name get-machine-user* :query :one-kebab
-- :doc Get the machine user by its id.
SELECT id, user_id, api_key
FROM machine_users
WHERE id = :id::uuid
  AND destroyed_at IS NULL
