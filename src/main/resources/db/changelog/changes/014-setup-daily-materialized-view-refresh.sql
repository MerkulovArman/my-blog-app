--liquibase formatted sql

--changeset arman:014-setup-daily-materialized-view-refresh splitStatements:false endDelimiter:/
--comment: Setup daily materialized view refresh using pg_cron

-- Create extension if not exists (requires superuser or rds_superuser role)
-- This should be done manually in production
-- CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Function to refresh materialized view with logging
CREATE OR REPLACE FUNCTION refresh_active_users_mv_scheduled()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY active_users_stats_mv;
    
    INSERT INTO materialized_view_refresh_log (view_name, refresh_type, triggered_at, success)
    VALUES ('active_users_stats_mv', 'SCHEDULED', CURRENT_TIMESTAMP, true);
    
EXCEPTION WHEN OTHERS THEN
    INSERT INTO materialized_view_refresh_log (view_name, refresh_type, triggered_at, success, error_message)
    VALUES ('active_users_stats_mv', 'SCHEDULED', CURRENT_TIMESTAMP, false, SQLERRM);
    
    RAISE;
END;
$$ LANGUAGE plpgsql;

-- Function to setup daily pg_cron job
CREATE OR REPLACE FUNCTION setup_daily_mv_refresh_job()
RETURNS TEXT AS $$
DECLARE
    job_id INTEGER;
    existing_job_count INTEGER;
BEGIN
    -- Check if pg_cron extension is available
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        RETURN 'pg_cron extension not available. Install it manually: CREATE EXTENSION pg_cron;';
    END IF;

    -- Check if job already exists
    SELECT COUNT(*) INTO existing_job_count
    FROM cron.job
    WHERE command LIKE '%refresh_active_users_mv_scheduled%';

    IF existing_job_count > 0 THEN
        RETURN 'Daily materialized view refresh job already exists';
    END IF;

    -- Schedule daily job at midnight UTC
    SELECT cron.schedule(
        'daily-active-users-mv-refresh-job',
        '0 0 * * *',
        'SELECT refresh_active_users_mv_scheduled();'
    ) INTO job_id;

    RETURN 'Successfully scheduled daily materialized view refresh job with ID: ' || job_id;

EXCEPTION WHEN OTHERS THEN
    RETURN 'Error setting up daily job: ' || SQLERRM;
END;
$$ LANGUAGE plpgsql;
-- Try to setup the daily job (will fail gracefully if pg_cron not available)
-- This is commented out to avoid errors during initial setup
-- SELECT setup_daily_mv_refresh_job();

-- Add comment
COMMENT ON FUNCTION refresh_active_users_mv_scheduled() IS 'Refresh active users materialized view with error handling and logging';
COMMENT ON FUNCTION setup_daily_mv_refresh_job() IS 'Setup daily pg_cron job for materialized view refresh';
/
--rollback DROP FUNCTION IF EXISTS refresh_active_users_mv_scheduled();
--rollback DROP FUNCTION IF EXISTS setup_daily_mv_refresh_job();