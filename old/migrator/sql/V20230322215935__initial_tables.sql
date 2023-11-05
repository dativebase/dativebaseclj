CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE olds (
  "slug" text PRIMARY KEY,
  "name" text NOT NULL,
  "created_at" timestamp with time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp with time zone DEFAULT now() NOT NULL,
  "destroyed_at" timestamp with time zone
);

CREATE TABLE events (
  "id" uuid PRIMARY KEY DEFAULT (uuid_generate_v4()),
  "created_at" timestamp with time zone DEFAULT now(),
  "old_slug" text,
  "table_name" text NOT NULL,
  "row_id" uuid,
  "row_data" text NOT NULL,
  CONSTRAINT events_check_old_slug_or_row_id CHECK (old_slug IS NOT NULL OR row_id IS NOT NULL)
);

CREATE TABLE users (
  "id" uuid PRIMARY KEY DEFAULT (uuid_generate_v4()),
  "first_name" text NOT NULL,
  "last_name" text NOT NULL,
  "email" text NOT NULL,
  "username" text NOT NULL,
  "password" text NOT NULL,
  "created_at" timestamp with time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp with time zone DEFAULT now() NOT NULL,
  "destroyed_at" timestamp with time zone
);

CREATE TABLE machine_users (
  "id" uuid PRIMARY KEY DEFAULT (uuid_generate_v4()),
  "user_id" uuid NOT NULL,
  "api_key" text NOT NULL,
  "created_at" timestamp with time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp with time zone DEFAULT now() NOT NULL,
  "destroyed_at" timestamp with time zone,
  CONSTRAINT fk_machine_users_user_id FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE users_olds (
  "id" uuid PRIMARY KEY DEFAULT (uuid_generate_v4()),
  "user_id" uuid NOT NULL,
  "old_slug" text NOT NULL,
  "role" text NOT NULL,
  "created_at" timestamp with time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp with time zone DEFAULT now() NOT NULL,
  "destroyed_at" timestamp with time zone,
  CONSTRAINT fk_users_olds_user_id_to_user_id FOREIGN KEY(user_id) REFERENCES users(id),
  CONSTRAINT fk_users_olds_old_slug_to_old_slug FOREIGN KEY(old_slug) REFERENCES olds(slug),
  CONSTRAINT users_olds_unique UNIQUE (user_id, old_slug, destroyed_at)
);

-- TODO: add an index to support arbitrary regular expression search across phonetic textual fields
-- https://www.alibabacloud.com/blog/postgresql-responds-to-fuzzy-or-regular-expression-based-searches-within-seconds_595637
CREATE TABLE forms (
  "id" uuid PRIMARY KEY DEFAULT (uuid_generate_v4()),
  "old_slug" text NOT NULL,
  "transcription" text NOT NULL,
  "inserted_at" timestamp with time zone DEFAULT now() NOT NULL,
  "created_at" timestamp with time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp with time zone DEFAULT now() NOT NULL,
  "destroyed_at" timestamp with time zone,
  "created_by_user_id" uuid NOT NULL,
  CONSTRAINT fk_forms_old_slug FOREIGN KEY(old_slug) REFERENCES olds(slug),
  CONSTRAINT fk_forms_created_by_user_id FOREIGN KEY(created_by_user_id) REFERENCES users(id)
);

CREATE INDEX forms_transcription_trgm_idx ON forms USING GIN (transcription gin_trgm_ops);
-- Or ...
-- CREATE INDEX forms_transcription_trgm_idx ON forms USING GIN (transcription gin_trgm_ops) with (fastupdate='on', gin_pending_list_limit='6553600');
-- https://www.alibabacloud.com/blog/postgresql-responds-to-fuzzy-or-regular-expression-based-searches-within-seconds_595637

CREATE INDEX events_history_idx ON events(old_slug, "table_name", row_id);

CREATE INDEX forms_old_slug_idx ON forms(old_slug);

CREATE INDEX forms_inserted_at_id_idx ON forms(inserted_at, id);

CREATE INDEX machine_users_user_id_idx ON machine_users(user_id);
