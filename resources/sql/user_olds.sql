-- :name create-user-old* :returning-execute :one-kebab
-- :doc Create a new user-old: the relationship between a user and an OLD.
INSERT INTO users_olds (user_id,
                        old_slug,
                        role,
                        created_by,
                        updated_by)
  VALUES (:user-id,
          :old-slug,
          :role,
          :created-by,
          :updated-by)
  RETURNING *

-- :name get-user-old* :query :one-kebab
-- :doc Get a user OLD by its ID.
SELECT *
  FROM users_olds
  WHERE id = :id::uuid
    AND destroyed_at IS NULL

-- :name get-user-olds* :query :many-kebab
-- :doc Get all user OLDs, ordered by inserted_at.
SELECT *
  FROM users_olds
  WHERE destroyed_at IS NULL
  ORDER BY inserted_at, id
  LIMIT :limit
  OFFSET :offset

-- :name update-user-old* :returning-execute :one-kebab
-- :doc Update the role of the specified user-old.
UPDATE users_olds
  SET role = :role,
      updated_at = now(),
      updated_by = :updated-by
  WHERE id = :id::uuid
  RETURNING *

-- :name delete-user-old* :returning-execute :one-kebab
-- :doc Soft-delete the user-old referenced by ID.
UPDATE users_olds
  SET destroyed_at = now(),
      updated_at = now(),
      updated_by = :updated-by
  WHERE id = :id::uuid
  RETURNING *
