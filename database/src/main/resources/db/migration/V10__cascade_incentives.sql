ALTER TABLE incentives
    DROP CONSTRAINT"incentives_gameId_fkey",
    ADD CONSTRAINT "incentives_gameId_fkey"
        FOREIGN KEY ("gameId")
        REFERENCES gamedata(id)
        ON DELETE CASCADE;

ALTER TABLE chosen_incentives
    DROP CONSTRAINT chosen_incentives_incentive_id_fkey,
    ADD CONSTRAINT chosen_incentives_incentive_id_fkey
        FOREIGN KEY (incentive_id)
        REFERENCES incentives(id)
        ON DELETE CASCADE;

ALTER TABLE chosen_incentives
    DROP CONSTRAINT chosen_incentives_incentive_code_fkey,
    ADD CONSTRAINT chosen_incentives_incentive_code_fkey
        FOREIGN KEY (incentive_code)
        REFERENCES incentive_codes(id)
        ON DELETE CASCADE;

