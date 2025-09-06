--liquibase formatted sql

--changeset myblog:004-create-post-tags-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'post_tags' AND table_schema = current_schema()

CREATE TABLE post_tags (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Создание индексов для оптимизации поиска
CREATE INDEX idx_post_tags_post_id ON post_tags(post_id);
CREATE INDEX idx_post_tags_tag_id ON post_tags(tag_id);

-- Комментарии к таблице и полям
COMMENT ON TABLE post_tags IS 'Связующая таблица между постами и тегами';
COMMENT ON COLUMN post_tags.post_id IS 'ID поста';
COMMENT ON COLUMN post_tags.tag_id IS 'ID тега';

--rollback DROP TABLE post_tags;