CREATE OR REPLACE FUNCTION notify_change() RETURNS TRIGGER AS
$$
DECLARE
    payload     JSON;
    field_value TEXT;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        EXECUTE format('SELECT ($1).%I FROM (SELECT NEW) AS t', TG_ARGV[0]) INTO field_value USING NEW;
        payload := json_build_object(
                'operation', TG_OP,
                'id', field_value
                   );
    ELSE
        field_value := NEW.(TG_ARGV[0]);
        payload := json_build_object(
                'operation', TG_OP,
                'data', json_build_object(TG_ARGV[0], field_value)
                   );
    END IF;
    PERFORM pg_notify(TG_TABLE_NAME || '_change', payload::text);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION add_notification_triggers(table_name TEXT, field_name TEXT) RETURNS VOID AS
$$
BEGIN
    EXECUTE format('
    CREATE TRIGGER %I_notify_change
    AFTER INSERT OR UPDATE OR DELETE ON %I
    FOR EACH ROW EXECUTE FUNCTION notify_change(%L);',
                   table_name || '_notify_change', table_name, field_name);
END;
$$ LANGUAGE plpgsql;

DO $$
    BEGIN
        PERFORM add_notification_triggers('gamedata', 'id');
    END $$;