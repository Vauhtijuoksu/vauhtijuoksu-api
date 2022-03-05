CREATE TABLE player_info (
    id BOOLEAN DEFAULT true PRIMARY KEY,
    message TEXT,
    CONSTRAINT only_one_allowed CHECK (id)
);

INSERT INTO player_info values (true, null)
