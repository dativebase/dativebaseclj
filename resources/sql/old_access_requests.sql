-- :name create-old-access-request* :returning-execute :one-kebab
-- :doc Create a new OLD access request
INSERT INTO old_access_requests (old_slug,
                                 user_id)
  VALUES (:old-slug,
          :user-id)
  RETURNING *

-- :name approve* :returning-execute :one-kebab
-- :doc Approve the OLD access request
UPDATE old_access_requests
  SET status = 'approved'
  WHERE id = :id
  RETURNING *

-- :name reject* :returning-execute :one-kebab
-- :doc Reject the OLD access request
UPDATE old_access_requests
  SET status = 'rejected'
  WHERE id = :id
  RETURNING *

-- :name retract* :returning-execute :one-kebab
-- :doc Retract the OLD access request
UPDATE old_access_requests
  SET status = 'retracted'
  WHERE id = :id
  RETURNING *

-- :name get-old-access-request* :query :one-kebab
-- :doc Get an OLD access request by its ID.
SELECT *
  FROM old_access_requests
  WHERE id = :id

-- :name get-pending-old-access-requests-for-user* :query :many-kebab
-- :doc Get all pending OLD access requests for the specified user ID.
SELECT *
  FROM old_access_requests
  WHERE user_id = :user-id::uuid
    AND status = 'pending'
  ORDER BY created_at, id

-- :name get-pending-old-access-requests-for-old* :query :many-kebab
-- :doc Get all pending OLD access requests for the specified OLD slug.
SELECT *
  FROM old_access_requests
  WHERE old_slug = :old-slug
    AND status = 'pending'
  ORDER BY created_at, id

-- :name get-old-access-requests* :query :many-kebab
-- :doc Get all OLD access requests, ordered by created_at.
SELECT *
  FROM old_access_requests
  ORDER BY created_at, id
  LIMIT :limit
  OFFSET :offset

-- :name count-old-access-requests* :query :one-kebab
-- :doc Get the count of OLD access requests in the entire database.
SELECT count(id) AS old_access_request_count
  FROM old_access_requests
