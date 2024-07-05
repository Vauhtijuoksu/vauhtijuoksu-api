-- Step 1: Create the shared counter table
CREATE TABLE shared_counter (
                                model TEXT PRIMARY KEY,
                                counter INTEGER DEFAULT 0
);

-- Step 2: Create the function to increment the shared counter
CREATE OR REPLACE FUNCTION increment_shared_counter() RETURNS TRIGGER AS
$$
BEGIN
    UPDATE shared_counter
    SET counter = counter + 1
    WHERE model = TG_ARGV[0];
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 3: Create the reusable function to add triggers
CREATE OR REPLACE FUNCTION add_change_trigger(table_name TEXT, model_name TEXT) RETURNS VOID AS
$$
BEGIN
    -- Insert the model into the shared_counter table if it does not exist
    EXECUTE format('
        INSERT INTO shared_counter (model, counter) VALUES (%L, 0)
        ON CONFLICT (model) DO NOTHING;',
                   model_name);

    -- Create the trigger for the specified table
    EXECUTE format('
        CREATE TRIGGER %I_change_trigger
        AFTER INSERT OR UPDATE OR DELETE ON %I
        FOR EACH ROW
        EXECUTE FUNCTION increment_shared_counter(%L);',
                   table_name || '_change', table_name, model_name);
END;
$$ LANGUAGE plpgsql;

-- Step 4: Use the function to add triggers
-- Add triggers for the gamedata table
SELECT add_change_trigger('gamedata', 'gamedata');

-- Add triggers for the participants_in_game table
SELECT add_change_trigger('participant_in_game', 'gamedata');
