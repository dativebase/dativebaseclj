CREATE TABLE old_access_requests (
  id uuid PRIMARY KEY DEFAULT (uuid_generate_v4()),
  old_slug text NOT NULL,
  user_id uuid NOT NULL,
  status text DEFAULT 'pending'::text NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  CONSTRAINT fk_old_access_request_user_id FOREIGN KEY(user_id) REFERENCES users(id),
  CONSTRAINT fk_old_access_request_old_slug FOREIGN KEY(old_slug) REFERENCES olds(slug)
)
