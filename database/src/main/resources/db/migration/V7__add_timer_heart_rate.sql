ALTER TABLE stream_metadata ADD COLUMN heart_rates int[];

UPDATE stream_metadata SET heart_rates = '{}';

CREATE TABLE timers (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE
);
