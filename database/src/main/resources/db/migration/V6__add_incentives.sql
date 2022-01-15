CREATE TYPE INCENTIVE_TYPE AS ENUM ('MILESTONE', 'OPTION', 'OPEN');

CREATE TABLE incentives (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    "gameId" uuid REFERENCES gamedata(id) ON DELETE cascade,
    title TEXT NOT NULL,
    "endTime" TIMESTAMP WITH TIME ZONE,
    type INCENTIVE_TYPE,
    info TEXT,
    milestones INT[],
    "optionParameters" TEXT[],
    "openCharLimit" INT
);
