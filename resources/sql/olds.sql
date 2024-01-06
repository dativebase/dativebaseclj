-- :name create-old* :returning-execute :one-kebab
INSERT INTO olds (slug,
                  name,
                  plan_id,
                  created_by,
                  updated_by)
  VALUES (:slug,
          :name,
          :plan-id,
          :created-by,
          :updated-by)
  RETURNING *

-- :name update-old* :returning-execute :one-kebab
UPDATE olds
  SET name = :name,
      plan_id = :plan-id,
      updated_at = now(),
      updated_by = :updated-by
  WHERE slug = :slug
  RETURNING *

-- :name get-old* :query :one-kebab
-- :doc Get a old by its slug.
SELECT *
  FROM olds
  WHERE slug = :slug
    AND destroyed_at IS NULL

-- :name get-old-with-users* :query :many-kebab
-- :doc Return a coll of the active users for the referenced OLD.
SELECT o.*,
       uo.role,
       uo.user_id,
       uo.id AS user_old_id
  FROM olds o
  INNER JOIN users_olds uo
    ON o.slug = uo.old_slug
      AND uo.destroyed_at IS NULL
  WHERE o.destroyed_at IS NULL
    AND o.slug = :slug
  ORDER BY o.inserted_at, o.slug

-- :name delete-old* :returning-execute :one-kebab
-- :doc Delete the referenced OLD.
UPDATE olds
  SET destroyed_at = now(),
      updated_at = now(),
      updated_by = :updated-by
  WHERE slug = :slug
  RETURNING *
