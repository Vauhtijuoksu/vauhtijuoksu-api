ALTER TABLE players_in_game ADD COLUMN player_order smallint DEFAULT 0 NOT NULL;
ALTER TABLE players_in_game ALTER COLUMN player_order drop DEFAULT;
