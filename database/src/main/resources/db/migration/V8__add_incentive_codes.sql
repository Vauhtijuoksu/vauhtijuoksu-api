CREATE TABLE incentive_codes (
    id char(9) PRIMARY KEY
);

CREATE TABLE chosen_incentives (
    incentive_id UUID REFERENCES incentives(id),
    incentive_code char(9) REFERENCES incentive_codes(id),
    parameter TEXT
);
