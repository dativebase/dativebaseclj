-- :name insert-event* :returning-execute-kebab
INSERT INTO events (old_slug, "table_name", row_id, row_data)
VALUES (:old-slug, :table-name, :row-id, :row-data)
RETURNING id, created_at, old_slug, "table_name", row_id, row_data

-- :name get-history* :query :many-kebab
-- :doc Get the history of a domain entity by providing its ID (:row-id), the entity's table name, and the ID of the OLD to which the entity belongs.
SELECT id, created_at, old_slug, table_name, row_id, row_data
FROM events
WHERE table_name = :table-name
  AND old_slug = :old-slug
  AND row_id = :row-id::uuid
ORDER BY created_at DESC
LIMIT :limit

-- :name get-history-of-global* :query :many-kebab
-- :doc Get the history of a global (i.e., non-OLD-specific) entity (e.g., a user) by providing its ID (:row-id) and the entity's table name.
SELECT id, created_at, old_slug, table_name, row_id, row_data
FROM events
WHERE table_name = :table-name
  AND old_slug IS NULL
  AND row_id = :row-id::uuid
ORDER BY created_at DESC
LIMIT :limit

-- :name get-history-of-old* :query :many-kebab
-- :doc Get the history of an OLD by providing its slug.
SELECT id, created_at, old_slug, table_name, row_id, row_data
FROM events
WHERE table_name = 'olds'
  AND old_slug = :old-slug
  AND row_id IS NULL
ORDER BY created_at DESC
LIMIT :limit
