# Daily Materialized View Refresh System

## Overview

This document describes the implementation of a PostgreSQL-based daily refresh system for materialized views in the MyBlogApp. The system has been redesigned to use database-level scheduling with pg_cron instead of application-level scheduling.

## Key Changes

### 1. Database Migration (014-setup-daily-materialized-view-refresh.sql)

**New Functions Created:**

- `refresh_active_users_mv()`: Core function to refresh the materialized view with error handling and logging
- `setup_daily_mv_refresh_job()`: Sets up pg_cron job for daily refresh at midnight (00:00 UTC)
- `remove_daily_mv_refresh_job()`: Removes the daily refresh job
- `get_daily_mv_refresh_job_status()`: Returns status information about the daily job

**Automatic Setup:**
The migration attempts to automatically set up the daily refresh job during deployment.

### 2. MaterializedViewService Updates

**Scheduling Changes:**
- Changed from 30-minute intervals to daily fallback scheduling (24 hours)
- Added intelligent checking for active database jobs to avoid duplicate refreshes
- Updated all methods to use the new database functions

**Key Methods:**
- `initializeDatabaseScheduler()`: Sets up daily pg_cron job at 00:00 UTC
- `refreshActiveUsersStatistics()`: Now serves as fallback only (runs daily)
- `forceRefreshActiveUsersStatistics()`: Uses database function for consistency
- `getCronJobStatus()`: Returns detailed status of the daily job
- `isDailyJobActive()`: Checks if database-level job is running

### 3. MaterializedViewController Enhancements

**Updated Endpoints:**

- `POST /private/materialized-views/initialize-scheduler`: Sets up daily refresh at midnight
- `POST /private/materialized-views/disable-scheduler`: Removes daily database job
- `GET /private/materialized-views/cron-status`: Shows daily job status
- `GET /private/materialized-views/health`: Comprehensive system status

**Enhanced Responses:**
All endpoints now provide more detailed information about the daily scheduling system.

## Usage Instructions

### Setting Up Daily Refresh

1. **Automatic Setup**: The daily job is automatically configured during database migration
2. **Manual Setup**: Call the initialization endpoint:
   ```
   POST /api/private/materialized-views/initialize-scheduler
   ```

### Managing the Daily Job

**Check Status:**
```
GET /api/private/materialized-views/health
```

**View Job Details:**
```
GET /api/private/materialized-views/cron-status
```

**Disable Daily Job:**
```
POST /api/private/materialized-views/disable-scheduler
```

**Force Immediate Refresh:**
```
POST /api/private/materialized-views/refresh
```

## System Architecture

### Database Level (Optimal)
- **Schedule**: Daily at 00:00 UTC
- **Technology**: PostgreSQL pg_cron extension
- **Function**: `refresh_active_users_mv()`
- **Logging**: Comprehensive error handling and performance tracking

### Application Level (Fallback)
- **Schedule**: Daily fallback via @Scheduled annotation
- **Condition**: Only runs if database job is not active
- **Purpose**: Ensures refresh happens even without pg_cron

## Status Types

The system reports the following status types in `/health` endpoint:

- **OPTIMAL**: pg_cron available and daily job is active
- **AVAILABLE**: pg_cron available but job not configured
- **FALLBACK**: Using application-level daily scheduling

## Benefits of Daily Refresh

1. **Performance**: Materialized views provide fast query performance for user statistics
2. **Data Freshness**: Daily updates ensure statistics are current (last 10 days of activity)
3. **Database Efficiency**: Refresh happens at low-traffic time (midnight UTC)
4. **Reliability**: Dual-level scheduling ensures refresh happens regardless of system state
5. **Monitoring**: Comprehensive logging and status reporting

## Configuration

### pg_cron Requirements
- PostgreSQL with pg_cron extension installed
- Superuser privileges for initial setup (automatic during migration)
- Cron daemon running on database server

### Fallback Configuration
- No additional configuration needed
- Automatically activates if pg_cron is unavailable
- Uses Spring @Scheduled annotation

## Monitoring and Logs

### Database Logs
All refresh operations are logged in:
- `materialized_view_refresh_log`: Detailed refresh logs with timing
- `system_log`: General system operation logs

### Application Logs
- Refresh operations logged at INFO level
- Error conditions logged at ERROR level
- Status checks logged at DEBUG level

## Troubleshooting

### Common Issues

1. **pg_cron not available**: System automatically falls back to application-level scheduling
2. **Job not running**: Use initialization endpoint to set up the job
3. **Permission issues**: Requires database superuser privileges for pg_cron setup

### Diagnostic Commands

```sql
-- Check if pg_cron is available
SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname = 'pg_cron');

-- View current jobs
SELECT * FROM cron.job WHERE jobname = 'daily_active_users_mv_refresh';

-- Check recent refresh logs
SELECT * FROM materialized_view_refresh_log 
WHERE view_name = 'active_users_stats_mv' 
ORDER BY triggered_at DESC LIMIT 10;
```

## Migration Path

The system maintains backward compatibility:
- Existing materialized views continue to work
- Old logging systems remain functional
- Gradual transition from frequent to daily refreshes

This implementation provides a robust, efficient, and monitorable solution for keeping materialized view data fresh while minimizing system overhead.