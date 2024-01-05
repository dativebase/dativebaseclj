UPDATE olds
  SET plan_id = NULL
  WHERE plan_id IS NOT NULL;

ALTER TABLE olds
  ADD CONSTRAINT fk_olds_plan_id FOREIGN KEY (plan_id) REFERENCES plans(id);
