-- :name create-form* :returning-execute :one-kebab
INSERT INTO forms (old_slug, transcription, created_by, updated_by)
VALUES (:old-slug, :transcription, :created-by, :updated-by)
RETURNING *

-- :name update-form* :returning-execute :one-kebab
UPDATE forms
SET transcription = :transcription,
    updated_at = now(),
    updated_by = :updated-by
WHERE id = :id::uuid
RETURNING *

-- :name get-form* :query :one-kebab
-- :doc Get a form by its id.
SELECT *
FROM forms
WHERE id = :id::uuid
  AND destroyed_at IS NULL

-- :name delete-form* :returning-execute :one-kebab
UPDATE forms
SET destroyed_at = now(),
    updated_at = now(),
    updated_by = :updated-by
WHERE id = :id::uuid
RETURNING *

-- :name get-forms* :query :many-kebab
-- :doc Get all forms, ordered by inserted_at.
SELECT *
FROM forms
WHERE old_slug = :old-slug
  AND destroyed_at IS NULL
ORDER BY inserted_at, id
LIMIT :limit
OFFSET :offset

-- :name count-forms* :query :one-kebab
-- :doc Get the count of forms for the given OLD.
SELECT count(id) AS form_count
FROM forms
WHERE old_slug = :old-slug
  AND destroyed_at IS NULL
