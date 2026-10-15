CREATE TABLE charities (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name TEXT NOT NULL,
    url TEXT,
    goal INT,
    img_filename TEXT
);

-- A charity with donations can't be deleted
ALTER TABLE donations ADD COLUMN charity UUID REFERENCES charities(id);
