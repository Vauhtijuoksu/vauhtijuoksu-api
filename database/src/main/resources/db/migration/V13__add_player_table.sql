CREATE TABLE players (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    display_name TEXT,
    twitch_channel TEXT,
    discord_nick TEXT
);

CREATE TABLE players_in_game (
    game_id UUID references gamedata(id) ON DELETE CASCADE,
    player_id UUID references players(id) ON DELETE CASCADE,
    PRIMARY KEY(game_id, player_id)
);

ALTER TABLE gamedata DROP COLUMN player;
ALTER TABLE gamedata DROP COLUMN player_twitch;
