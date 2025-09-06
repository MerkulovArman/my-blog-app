--liquibase formatted sql

--changeset myblog:003-create-tags-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'tags' AND table_schema = current_schema()

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    usage_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов для оптимизации поиска
CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_tags_usage_count ON tags(usage_count DESC);
CREATE INDEX idx_tags_is_active ON tags(is_active);

-- Комментарии к таблице и полям
COMMENT ON TABLE tags IS 'Таблица тегов для постов';
COMMENT ON COLUMN tags.name IS 'Название тега (уникальное)';
COMMENT ON COLUMN tags.description IS 'Описание тега';
COMMENT ON COLUMN tags.usage_count IS 'Количество использований тега';
COMMENT ON COLUMN tags.is_active IS 'Активен ли тег';

--rollback DROP TABLE tags;