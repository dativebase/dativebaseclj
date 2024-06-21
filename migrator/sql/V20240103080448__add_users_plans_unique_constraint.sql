ALTER TABLE users_plans
  ADD CONSTRAINT users_plans_unique
  UNIQUE NULLS NOT DISTINCT (user_id, plan_id, destroyed_at);
