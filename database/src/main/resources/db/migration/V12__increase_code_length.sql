ALTER TABLE incentive_codes ALTER COLUMN id TYPE char(11);

ALTER TABLE chosen_incentives ALTER COLUMN incentive_code TYPE char(11);
