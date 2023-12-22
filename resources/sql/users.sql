-- :name create-user* :returning-execute :one-kebab
INSERT INTO users (first_name,
                   last_name,
                   email,
                   password,
                   is_superuser,
                   created_by,
                   updated_by)
VALUES (:first-name,
        :last-name,
        :email,
        :password,
        :is-superuser?,
        :created-by,
        :updated-by)
RETURNING *

-- :name update-user* :returning-execute :one-kebab
UPDATE users
SET first_name = :first-name,
    last_name = :last-name,
    email = :email,
    is_superuser = :is-superuser?,
    updated_at = now(),
    updated_by = :updated-by
WHERE id = :id::uuid
RETURNING *

-- :name get-user* :query :one-kebab
-- :doc Get a user by its id.
SELECT *
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
SELECT u.*, uo.role, uo.old_slug
FROM users u
LEFT OUTER JOIN users_olds uo
  ON uo.user_id = u.id
    AND uo.destroyed_at IS NULL
WHERE u.id = :id::uuid
  AND u.destroyed_at IS NULL

-- :name delete-user* :returning-execute :one-kebab
UPDATE users
SET destroyed_at = now(),
    updated_at = now(),
    updated_by = :updated-by
WHERE id = :id::uuid
RETURNING *

-- :name create-user-old* :returning-execute :one-kebab
INSERT INTO users_olds (user_id, old_slug, role, created_by, updated_by)
VALUES (:user-id, :old-slug, :role, :created-by, :updated-by)
RETURNING *

-- :name update-user-old* :returning-execute :one-kebab
UPDATE users_olds
SET role = :role,
    updated_at = now(),
    updated_by = :updated-by
WHERE id = :id::uuid
RETURNING *

-- :name delete-user-old* :returning-execute :one-kebab
UPDATE users_olds
SET destroyed_at = now(),
    updated_at = now(),
    updated_by = :updated-by
WHERE id = :id::uuid
RETURNING *

-- :name get-users* :query :many-kebab
-- :doc Get all users, ordered by created_at.
SELECT *
  FROM users
 WHERE destroyed_at IS NULL
 ORDER BY inserted_at, id
 LIMIT :limit
OFFSET :offset

-- :name count-users* :query :one-kebab
-- :doc Get the count of users in the entire database.
SELECT count(id) AS user_count
  FROM users
 WHERE destroyed_at IS NULL
