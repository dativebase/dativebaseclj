CREATE TABLE plans (
  id uuid PRIMARY KEY DEFAULT (uuid_generate_v4()),
  tier character varying(200) DEFAULT 'free' NOT NULL,
  inserted_at timestamp with time zone DEFAULT now() NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  destroyed_at timestamp with time zone,
  created_by uuid NOT NULL,
  updated_by uuid NOT NULL,
  CONSTRAINT fk_plans_created_by FOREIGN KEY(created_by) REFERENCES users(id),
  CONSTRAINT fk_plans_updated_by FOREIGN KEY(updated_by) REFERENCES users(id)
);

CREATE TABLE users_plans (
  id uuid PRIMARY KEY DEFAULT (uuid_generate_v4()),
  user_id uuid NOT NULL,
  plan_id uuid NOT NULL,
  role text NOT NULL,
  inserted_at timestamp with time zone DEFAULT now() NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL,
  updated_at timestamp with time zone DEFAULT now() NOT NULL,
  destroyed_at timestamp with time zone,
  created_by uuid NOT NULL,
  updated_by uuid NOT NULL,
  CONSTRAINT fk_users_plans_plan FOREIGN KEY(plan_id) REFERENCES plans(id),
  CONSTRAINT fk_users_plans_user FOREIGN KEY(user_id) REFERENCES users(id),
  CONSTRAINT fk_users_plans_created_by FOREIGN KEY(created_by) REFERENCES users(id),
  CONSTRAINT fk_users_plans_updated_by FOREIGN KEY(updated_by) REFERENCES users(id)
);
