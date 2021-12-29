CREATE TABLE stream_metadata (
    id boolean DEFAULT true PRIMARY KEY,
    donation_goal int,
    current_game_id uuid REFERENCES gamedata(id) ON DELETE set null,
    donatebar_info text[],
    counters int[],
    CONSTRAINT only_one_allowed CHECK (id)
);

INSERT INTO stream_metadata values (true, null, null, '{}', '{}')
