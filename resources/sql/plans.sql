-- :name create-plan* :returning-execute :one-kebab
INSERT INTO plans (tier, created_by, updated_by)
  VALUES (:tier, :created-by, :updated-by)
  RETURNING *

-- :name update-plan* :returning-execute :one-kebab
UPDATE plans
  SET tier = :tier,
      updated_at = now(),
      updated_by = :updated-by
  WHERE id = :id
  RETURNING *

-- :name get-plan* :query :one-kebab
-- :doc Get a plan by its ID.
SELECT *
  FROM plans
  WHERE id = :id::uuid
    AND destroyed_at IS NULL

-- :name get-plans* :query :many-kebab
-- :doc Get all plans, ordered by inserted_at.
SELECT *
  FROM plans
  WHERE destroyed_at IS NULL
  ORDER BY inserted_at, id
  LIMIT :limit
  OFFSET :offset

-- :name delete-plan* :returning-execute :one-kebab
UPDATE plans
  SET destroyed_at = now(),
      updated_at = now(),
      updated_by = :updated-by
  WHERE id = :id
  RETURNING *

-- :name get-plans-for-user* :query :many-kebab
-- :doc Get all plans for the referenced user.
SELECT p.*
  FROM plans p
  INNER JOIN users_plans up
  ON p.id = up.plan_id
    AND up.destroyed_at IS NULL
  WHERE p.destroyed_at IS NULL
    AND up.user_id = :user-id::uuid
  ORDER BY p.inserted_at, p.id
