-- :name create-old* :returning-execute-kebab
INSERT INTO olds (slug, name)
VALUES (:slug, :name)
RETURNING slug, name, created_at, updated_at, destroyed_at

-- :name update-old* :returning-execute-kebab
UPDATE olds
SET slug = :slug,
    name = :name,
    updated_at = now()
WHERE slug = :slug
RETURNING slug, name, created_at, updated_at, destroyed_at

-- :name get-old* :query :one-kebab
-- :doc Get a old by its slug.
SELECT slug, name, created_at, updated_at, destroyed_at
FROM olds
WHERE slug = :slug
  AND destroyed_at IS NULL

-- :name delete-old* :returning-execute
UPDATE olds
SET destroyed_at = now(),
    updated_at = now()
WHERE slug = :slug
RETURNING slug, name, created_at, updated_at, destroyed_at
