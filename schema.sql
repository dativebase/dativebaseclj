
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';

SET default_tablespace = '';

SET default_table_access_method = heap;

CREATE TABLE public.api_keys (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    key text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone NOT NULL
);

CREATE TABLE public.events (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    created_at timestamp with time zone DEFAULT now(),
    old_slug text,
    table_name text NOT NULL,
    row_id uuid,
    row_data text NOT NULL,
    CONSTRAINT events_check_old_slug_or_row_id CHECK (((old_slug IS NOT NULL) OR (row_id IS NOT NULL)))
);

CREATE TABLE public.forms (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    old_slug text NOT NULL,
    transcription text NOT NULL,
    inserted_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    destroyed_at timestamp with time zone,
    created_by uuid NOT NULL,
    updated_by uuid NOT NULL
);

CREATE TABLE public.olds (
    slug text NOT NULL,
    name text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    destroyed_at timestamp with time zone,
    created_by uuid,
    updated_by uuid,
    inserted_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.schema_version (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    email text NOT NULL,
    password text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    destroyed_at timestamp with time zone,
    is_superuser boolean DEFAULT false NOT NULL,
    created_by uuid,
    updated_by uuid,
    inserted_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.users_olds (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    old_slug text NOT NULL,
    role text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    destroyed_at timestamp with time zone,
    created_by uuid,
    updated_by uuid,
    inserted_at timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT api_keys_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.forms
    ADD CONSTRAINT forms_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.olds
    ADD CONSTRAINT olds_pkey PRIMARY KEY (slug);

ALTER TABLE ONLY public.schema_version
    ADD CONSTRAINT schema_version_pk PRIMARY KEY (installed_rank);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE NULLS NOT DISTINCT (email, destroyed_at);

ALTER TABLE ONLY public.users_olds
    ADD CONSTRAINT users_olds_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users_olds
    ADD CONSTRAINT users_olds_unique UNIQUE NULLS NOT DISTINCT (user_id, old_slug, destroyed_at);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX events_history_idx ON public.events USING btree (old_slug, table_name, row_id);

CREATE INDEX forms_inserted_at_id_idx ON public.forms USING btree (inserted_at, id);

CREATE INDEX forms_old_slug_idx ON public.forms USING btree (old_slug);

CREATE INDEX forms_transcription_trgm_idx ON public.forms USING gin (transcription public.gin_trgm_ops);

CREATE INDEX schema_version_s_idx ON public.schema_version USING btree (success);

CREATE INDEX users_email_idx ON public.users USING btree (email);

ALTER TABLE ONLY public.api_keys
    ADD CONSTRAINT fk_api_keys_user_id FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.forms
    ADD CONSTRAINT fk_forms_created_by_user_id FOREIGN KEY (created_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.forms
    ADD CONSTRAINT fk_forms_old_slug FOREIGN KEY (old_slug) REFERENCES public.olds(slug);

ALTER TABLE ONLY public.forms
    ADD CONSTRAINT fk_forms_updated_by_user_id FOREIGN KEY (updated_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.olds
    ADD CONSTRAINT fk_olds_created_by_user_id FOREIGN KEY (created_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.olds
    ADD CONSTRAINT fk_olds_updated_by_user_id FOREIGN KEY (updated_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_created_by_user_id FOREIGN KEY (created_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.users_olds
    ADD CONSTRAINT fk_users_olds_created_by_user_id FOREIGN KEY (created_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.users_olds
    ADD CONSTRAINT fk_users_olds_old_slug_to_old_slug FOREIGN KEY (old_slug) REFERENCES public.olds(slug);

ALTER TABLE ONLY public.users_olds
    ADD CONSTRAINT fk_users_olds_updated_by_user_id FOREIGN KEY (updated_by) REFERENCES public.users(id);

ALTER TABLE ONLY public.users_olds
    ADD CONSTRAINT fk_users_olds_user_id_to_user_id FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_updated_by_user_id FOREIGN KEY (updated_by) REFERENCES public.users(id);

