--liquibase formatted sql

--changeset myblog:002-create-posts-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'posts' AND table_schema = current_schema()

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    published_at TIMESTAMP,
    is_published BOOLEAN NOT NULL DEFAULT false,
    views_count BIGINT NOT NULL DEFAULT 0,
    search_vector tsvector,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Создание индексов для оптимизации поиска
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_is_published ON posts(is_published);
CREATE INDEX idx_posts_published_at ON posts(published_at);
CREATE INDEX idx_posts_created_at ON posts(created_at);
CREATE INDEX idx_posts_title ON posts USING gin(to_tsvector('russian', title));
CREATE INDEX idx_posts_content ON posts USING gin(to_tsvector('russian', content));

-- Составной индекс для эффективной выборки опубликованных постов
CREATE INDEX idx_posts_published_created_at ON posts(is_published, created_at DESC);

-- Комментарии к таблице и полям
COMMENT ON TABLE posts IS 'Таблица постов блога';
COMMENT ON COLUMN posts.title IS 'Заголовок поста';
COMMENT ON COLUMN posts.content IS 'Содержимое поста';
COMMENT ON COLUMN posts.published_at IS 'Дата и время публикации';
COMMENT ON COLUMN posts.is_published IS 'Опубликован ли пост';
COMMENT ON COLUMN posts.views_count IS 'Количество просмотров';
COMMENT ON COLUMN posts.search_vector IS 'Вектор для полнотекстового поиска';
COMMENT ON COLUMN posts.author_id IS 'ID автора поста';

--rollback DROP TABLE posts;