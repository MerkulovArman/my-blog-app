--liquibase formatted sql

--changeset myblog:009-create-gin-indexes-fulltext-search
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_posts_search_vector_gin' AND schemaname = current_schema()

-- Создание GIN индексов для полнотекстового поиска
-- Индекс для tsvector поля (будет обновляться триггером)
CREATE INDEX idx_posts_search_vector_gin ON posts USING gin(search_vector);

--changeset myblog:009-create-search-vector-function
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.routines WHERE routine_name = 'update_posts_search_vector' AND routine_schema = current_schema()

-- Создание функции для обновления search_vector поля
CREATE OR REPLACE FUNCTION update_posts_search_vector()
RETURNS TRIGGER AS '
BEGIN
    NEW.search_vector := 
        setweight(to_tsvector(''russian'', COALESCE(NEW.title, '''')), ''A'') ||
        setweight(to_tsvector(''russian'', COALESCE(NEW.content, '''')), ''B'');
    RETURN NEW;
END;
' LANGUAGE plpgsql;

--changeset myblog:009-create-search-vector-trigger
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.triggers WHERE trigger_name = 'update_posts_search_vector_trigger' AND event_object_table = 'posts'

-- Создание триггера для автоматического обновления search_vector
DROP TRIGGER IF EXISTS update_posts_search_vector_trigger ON posts;

CREATE TRIGGER update_posts_search_vector_trigger
    BEFORE INSERT OR UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION update_posts_search_vector();

-- Обновляем существующие записи (если они есть)
UPDATE posts SET updated_at = updated_at WHERE search_vector IS NULL;

-- Комментарии
COMMENT ON FUNCTION update_posts_search_vector() IS 'Функция для автоматического обновления поля search_vector при изменении title или content';

--rollback DROP TRIGGER IF EXISTS update_posts_search_vector_trigger ON posts; DROP FUNCTION IF EXISTS update_posts_search_vector(); DROP INDEX IF EXISTS idx_posts_search_vector_gin;