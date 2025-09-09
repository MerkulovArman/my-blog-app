--liquibase formatted sql

--changeset arman:012-create-active-users-materialized-view
--comment: Create materialized view for fast active user statistics (last 10 days)

-- Create simple materialized view for active user statistics
-- This is a simplified version that will work with Liquibase
CREATE MATERIALIZED VIEW IF NOT EXISTS active_users_stats_mv AS
SELECT 
    u.username,
    u.display_name,
    COALESCE(posts_data.posts_count, 0) as posts_count,
    COALESCE(comments_data.comments_count, 0) as comments_count,
    COALESCE(likes_data.likes_received, 0) as likes_received,
    COALESCE(posts_data.total_views, 0) as total_views,
    (COALESCE(posts_data.posts_count, 0) * 10.0 + COALESCE(comments_data.comments_count, 0) * 3.0 + COALESCE(likes_data.likes_received, 0) * 1.0) as activity_score
FROM users u
LEFT JOIN (
    SELECT 
        p.author_id,
        COUNT(p.id) as posts_count,
        SUM(p.views_count) as total_views
    FROM posts p
    WHERE p.is_published = true 
      AND p.published_at >= CURRENT_DATE - INTERVAL '10 days'
    GROUP BY p.author_id
) posts_data ON u.id = posts_data.author_id
LEFT JOIN (
    SELECT 
        c.author_id,
        COUNT(c.id) as comments_count
    FROM comments c
    WHERE c.created_at >= CURRENT_DATE - INTERVAL '10 days'
    GROUP BY c.author_id
) comments_data ON u.id = comments_data.author_id
LEFT JOIN (
    SELECT 
        p.author_id,
        COUNT(l.id) as likes_received
    FROM posts p
    JOIN likes l ON p.id = l.post_id
    WHERE l.created_at >= CURRENT_DATE - INTERVAL '10 days'
    GROUP BY p.author_id
) likes_data ON u.id = likes_data.author_id
WHERE u.is_active = true
  AND (COALESCE(posts_data.posts_count, 0) > 0 OR COALESCE(comments_data.comments_count, 0) > 0 OR COALESCE(likes_data.likes_received, 0) > 0)
ORDER BY activity_score DESC;

-- Create indexes on materialized view for fast querying
CREATE INDEX IF NOT EXISTS idx_active_users_stats_mv_activity_score 
ON active_users_stats_mv (activity_score DESC);

-- Add unique index on username (should be unique in users table)
-- This is required for REFRESH MATERIALIZED VIEW CONCURRENTLY to work
CREATE UNIQUE INDEX IF NOT EXISTS idx_active_users_stats_mv_username_unique
    ON active_users_stats_mv (username);

-- Create table for system logging if it doesn't exist
CREATE TABLE IF NOT EXISTS system_log (
    id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(100) NOT NULL,
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

--rollback DROP MATERIALIZED VIEW IF EXISTS active_users_stats_mv CASCADE;
--rollback DROP TABLE IF EXISTS system_log CASCADE;