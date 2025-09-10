--liquibase formatted sql

--changeset myblog:015-create-simple-refresh-function splitStatements:false endDelimiter:/
--comment: Create simple materialized view refresh function

-- Simple function to refresh materialized view (without complex error handling to avoid issues)
CREATE OR REPLACE FUNCTION refresh_active_users_mv()
RETURNS VOID AS $$
BEGIN
    -- Try concurrent refresh first, fallback to non-concurrent if it fails
    BEGIN
        REFRESH MATERIALIZED VIEW CONCURRENTLY active_users_stats_mv;
    EXCEPTION WHEN OTHERS THEN
        -- If concurrent refresh fails, use regular refresh
        REFRESH MATERIALIZED VIEW active_users_stats_mv;
    END;
    
    -- Simple logging
    INSERT INTO materialized_view_refresh_log (view_name, refresh_type, triggered_at, success)
    VALUES ('active_users_stats_mv', 'APPLICATION', CURRENT_TIMESTAMP, true);
END;
$$ LANGUAGE plpgsql;

-- Simple function to get refresh status
CREATE OR REPLACE FUNCTION get_mv_refresh_count()
RETURNS INTEGER AS $$
BEGIN
    RETURN (SELECT COUNT(*) FROM materialized_view_refresh_log 
            WHERE view_name = 'active_users_stats_mv'
            AND triggered_at >= CURRENT_DATE);
END;
$$ LANGUAGE plpgsql;

-- Add comment
COMMENT ON FUNCTION refresh_active_users_mv() IS 'Simple function to refresh active users materialized view';
COMMENT ON FUNCTION get_mv_refresh_count() IS 'Get count of refreshes today';
/
--rollback DROP FUNCTION IF EXISTS refresh_active_users_mv();
--rollback DROP FUNCTION IF EXISTS get_mv_refresh_count();