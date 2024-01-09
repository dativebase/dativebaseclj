-- :name create-user* :returning-execute :one-kebab
-- :doc Create a new user.
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
-- :doc Update the referenced user.
UPDATE users
  SET first_name = :first-name,
      last_name = :last-name,
      email = :email,
      is_superuser = :is-superuser?,
      updated_at = now(),
      updated_by = :updated-by
  WHERE id = :id::uuid
  RETURNING *

-- :name activate-user* :returning-execute :one-kebab
-- :doc Activate the referenced user by setting its registration status to registered.
UPDATE users
  SET registration_status = 'registered'
  WHERE id = :id::uuid
  RETURNING *

-- :name deactivate-user* :returning-execute :one-kebab
-- :doc Deactivate the referenced user by setting its registration status to deactivated.
UPDATE users
  SET registration_status = 'deactivated',
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
  LEFT OUTER JOIN olds o
    ON o.slug = uo.old_slug
      AND o.destroyed_at IS NULL
  WHERE u.id = :id::uuid
    AND u.destroyed_at IS NULL

-- :name get-user-with-olds* :query :many-kebab
-- :doc Get a user by its id, including details on all of the OLDs to which the user has access. Like get-user-with-roles* but with more data.
SELECT u.*,
       uo.role,
       uo.id AS user_old_id,
       uo.old_slug,
       o.name
  FROM users u
  LEFT OUTER JOIN users_olds uo
    ON uo.user_id = u.id
      AND uo.destroyed_at IS NULL
  LEFT OUTER JOIN olds o
    ON o.slug = uo.old_slug
      AND o.destroyed_at IS NULL
  WHERE u.id = :id::uuid
    AND u.destroyed_at IS NULL

-- :name get-user-with-plans* :query :many-kebab
-- :doc Return a coll of the active plans for the referenced user.
SELECT u.*,
       up.id AS user_plan_id,
       up.role,
       up.plan_id,
       p.tier
  FROM users u
  INNER JOIN users_plans up
    ON u.id = up.user_id
      AND up.destroyed_at IS NULL
  INNER JOIN plans p
    ON p.id = up.plan_id
      AND p.destroyed_at IS NULL
  WHERE u.destroyed_at IS NULL
    AND u.id = :id::uuid
  ORDER BY p.inserted_at, p.id

-- :name delete-user* :returning-execute :one-kebab
-- :doc Soft-delete the referenced user.
UPDATE users
  SET destroyed_at = now(),
      updated_at = now(),
      updated_by = :updated-by
  WHERE id = :id::uuid
  RETURNING *

-- :name get-users* :query :many-kebab
-- :doc Get all users, ordered by inserted_at.
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
