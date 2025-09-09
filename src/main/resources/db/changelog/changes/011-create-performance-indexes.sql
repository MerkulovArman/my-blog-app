--liquibase formatted sql

--changeset arman:011-create-performance-indexes
--comment: Create composite and covering indexes for enhanced performance

-- Composite index for frequently searched post fields (title, published status, published date)
-- This covers common queries that filter by publication status and sort by date
CREATE INDEX IF NOT EXISTS idx_posts_published_date_composite 
ON posts (is_published, published_at DESC, title) 
WHERE is_published = true;

-- Composite index for user posts queries
-- Covers queries that fetch posts by author and order by creation date
CREATE INDEX IF NOT EXISTS idx_posts_author_created_composite 
ON posts (author_id, created_at DESC);

-- Composite index for tag-based post searches
-- Optimizes queries that join posts with tags
CREATE INDEX IF NOT EXISTS idx_post_tags_composite 
ON post_tags (tag_id, post_id);

-- Covering index for post summaries
-- Includes all fields needed for post summary responses without table lookup
CREATE INDEX IF NOT EXISTS idx_posts_summary_covering 
ON posts (is_published, published_at DESC) 
INCLUDE (id, title, views_count, author_id, created_at, updated_at)
WHERE is_published = true;

-- Index for views count ordering (popular posts)
CREATE INDEX IF NOT EXISTS idx_posts_views_count 
ON posts (views_count DESC, published_at DESC) 
WHERE is_published = true;

-- Composite index for user activity queries
CREATE INDEX IF NOT EXISTS idx_posts_user_activity 
ON posts (author_id, published_at);

-- Index for comment activity
CREATE INDEX IF NOT EXISTS idx_comments_user_activity 
ON comments (author_id, created_at);

-- Index for like activity  
CREATE INDEX IF NOT EXISTS idx_likes_user_activity 
ON likes (user_id, created_at);

-- Covering index for tag statistics
CREATE INDEX IF NOT EXISTS idx_tags_statistics_covering 
ON tags (is_active) 
INCLUDE (id, name, usage_count)
WHERE is_active = true;

--rollback DROP INDEX IF EXISTS idx_posts_published_date_composite;
--rollback DROP INDEX IF EXISTS idx_posts_author_created_composite;
--rollback DROP INDEX IF EXISTS idx_post_tags_composite;
--rollback DROP INDEX IF EXISTS idx_posts_summary_covering;
--rollback DROP INDEX IF EXISTS idx_posts_views_count;
--rollback DROP INDEX IF EXISTS idx_posts_user_activity;
--rollback DROP INDEX IF EXISTS idx_comments_user_activity;
--rollback DROP INDEX IF EXISTS idx_likes_user_activity;
--rollback DROP INDEX IF EXISTS idx_tags_statistics_covering;