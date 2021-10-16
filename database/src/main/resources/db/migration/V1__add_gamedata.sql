CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE gamedata (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    game TEXT NOT NULL,
    player TEXT NOT NULL,
    /*
        Not ideal to use text for date, but this allows to use same serializer for DB and API
        and dates will probably be removed in future
    */
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    category TEXT NOT NULL,
    device TEXT NOT NULL,
    published TEXT NOT NULL,
    vod_link TEXT,
    img_filename TEXT,
    player_twitch TEXT
)
