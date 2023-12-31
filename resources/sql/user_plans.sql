-- :name create-user-plan* :returning-execute :one-kebab
-- :doc Create a new user plan.
INSERT INTO users_plans (user_id, plan_id, role, created_by, updated_by)
  VALUES (:user-id, :plan-id, :role, :created-by, :updated-by)
  RETURNING *

-- :name update-user-plan* :returning-execute :one-kebab
-- :doc Update the referenced user plan.
UPDATE users_plans
  SET role = :role,
      updated_at = now(),
      updated_by = :updated-by
  WHERE id = :id
  RETURNING *

-- :name get-user-plan* :query :one-kebab
-- :doc Get a user plan by its ID.
SELECT *
  FROM users_plans
  WHERE id = :id
    AND destroyed_at IS NULL

-- :name get-user-plans* :query :many-kebab
-- :doc Get all user plans, ordered by inserted_at.
SELECT *
  FROM users_plans
  WHERE destroyed_at IS NULL
  ORDER BY inserted_at, id
  LIMIT :limit
  OFFSET :offset

-- :name delete-user-plan* :returning-execute :one-kebab
-- :doc Soft-delete the referenced user plan.
UPDATE users_plans
  SET destroyed_at = now(),
      updated_at = now(),
      updated_by = :updated-by
  WHERE id = :id
  RETURNING *
