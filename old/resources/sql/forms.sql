-- :name create-form* :returning-execute-kebab
INSERT INTO forms (old_slug, created_by_user_id, transcription)
VALUES (:old-slug, :created-by-user-id, :transcription)
RETURNING id, old_slug, transcription, created_by_user_id, inserted_at, created_at, updated_at, destroyed_at

-- :name update-form* :returning-execute-kebab
UPDATE forms
SET transcription = :transcription,
    updated_at = now()
WHERE id = :id::uuid
RETURNING id, old_slug, transcription, created_by_user_id, inserted_at, created_at, updated_at, destroyed_at

-- :name get-form* :query :one-kebab
-- :doc Get a form by its id.
SELECT id, old_slug, transcription, created_by_user_id, inserted_at, created_at, updated_at, destroyed_at
FROM forms
WHERE id = :id::uuid
  AND destroyed_at IS NULL

-- :name delete-form* :returning-execute-kebab
UPDATE forms
SET destroyed_at = now(),
    updated_at = now()
WHERE id = :id::uuid
RETURNING id, old_slug, transcription, created_by_user_id, inserted_at, created_at, updated_at, destroyed_at

-- :name get-forms* :query :many-kebab
-- :doc Get all forms, ordered by inserted_at.
SELECT id, old_slug, transcription, created_by_user_id, inserted_at, created_at, updated_at, destroyed_at
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
