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

-- :name delete-old* :returning-execute :one-kebab
UPDATE olds
  SET destroyed_at = now(),
      updated_at = now(),
      updated_by = :updated-by
  WHERE slug = :slug
  RETURNING *
