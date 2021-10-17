CREATE TABLE donations (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    name TEXT NOT NULL,
    message TEXT,
    timestamp TEXT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    read boolean NOT NULL
)
