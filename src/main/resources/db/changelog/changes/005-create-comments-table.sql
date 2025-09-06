--liquibase formatted sql

--changeset myblog:005-create-comments-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'comments' AND table_schema = current_schema()

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    is_approved BOOLEAN NOT NULL DEFAULT true,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    author_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- Создание индексов для оптимизации поиска
CREATE INDEX idx_comments_author_id ON comments(author_id);
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_parent_comment_id ON comments(parent_comment_id);
CREATE INDEX idx_comments_is_approved ON comments(is_approved);
CREATE INDEX idx_comments_is_deleted ON comments(is_deleted);
CREATE INDEX idx_comments_created_at ON comments(created_at);

-- Составной индекс для эффективной выборки активных комментариев к посту
CREATE INDEX idx_comments_post_active ON comments(post_id, is_approved, is_deleted, created_at);

-- Комментарии к таблице и полям
COMMENT ON TABLE comments IS 'Таблица комментариев к постам';
COMMENT ON COLUMN comments.content IS 'Текст комментария';
COMMENT ON COLUMN comments.is_approved IS 'Одобрен ли комментарий модератором';
COMMENT ON COLUMN comments.is_deleted IS 'Удален ли комментарий';
COMMENT ON COLUMN comments.author_id IS 'ID автора комментария';
COMMENT ON COLUMN comments.post_id IS 'ID поста, к которому относится комментарий';
COMMENT ON COLUMN comments.parent_comment_id IS 'ID родительского комментария (для вложенных комментариев)';

--rollback DROP TABLE comments;