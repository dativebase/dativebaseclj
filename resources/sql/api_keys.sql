-- :name create-api-key* :returning-execute-kebab
-- :doc Create a new API key.
INSERT INTO api_keys (user_id, key, expires_at)
VALUES (:user-id, :key, :expires-at::timestamp)
RETURNING *

-- :name get-api-key* :query :one-kebab
-- :doc Get an API key by ID.
SELECT *
FROM api_keys
WHERE id = :id::uuid
AND expires_at > now()

-- :name delete-api-key* :returning-execute-kebab
UPDATE api_keys
SET expires_at = now()
WHERE id = :id::uuid
RETURNING *

-- :name get-api-keys-for-user* :query :many-kebab
-- :doc Get all active API keys for the referenced user.
SELECT *
FROM api_keys
WHERE user_id = :user-id::uuid
AND expires_at > now()
