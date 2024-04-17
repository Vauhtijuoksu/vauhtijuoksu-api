CREATE TABLE participants
(
    id           UUID PRIMARY KEY,
    display_name TEXT NOT NULL
);

CREATE TABLE social_medias
(
    participant_id UUID references participants (id) ON DELETE CASCADE,
    platform       TEXT NOT NULL,
    username       TEXT NOT NULL
);

CREATE TABLE participant_in_game
(
    game_id           UUID references gamedata (id) ON DELETE CASCADE,
    participant_id    UUID references participants (id) ON DELETE CASCADE,
    role_in_game      TEXT     NOT NULL,
    participant_order smallint NOT NULL,
    PRIMARY KEY (game_id, participant_id, role_in_game)
);

INSERT INTO participants (id, display_name)
SELECT players.id,
       players.display_name
FROM players;

INSERT INTO social_medias (participant_id, platform, username)
SELECT players.id,
       'DISCORD',
       players.discord_nick
FROM players
WHERE discord_nick IS NOT NULL;

INSERT INTO social_medias (participant_id, platform, username)
SELECT players.id,
       'TWITCH',
       players.twitch_channel
FROM players
WHERE twitch_channel IS NOT NULL;

INSERT INTO participant_in_game (game_id, participant_id, role_in_game, participant_order)
SELECT game_id, player_id, 'PLAYER', player_order
FROM players_in_game;
